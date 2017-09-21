package controllers

import config.FrontendAuthConnector
import connectors.deskpro.HmrcDeskproConnector
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.validators.Validators._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future
import uk.gov.hmrc.play.HeaderCarrierConverter

trait ProblemReportsController extends FrontendController with ContactFrontendActions {

  def hmrcDeskproConnector: HmrcDeskproConnector
  def authConnector: AuthConnector

  val form = Form[ProblemReport](
    mapping(
      "report-name" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70)
        .verifying("error.common.problem_report.name_invalid_characters", name => name.matches( """^[A-Za-z\-.,()'"\s]+$""")),
      "report-email" -> emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),
      "report-action" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.comments_too_long", action => action.size <= 1000),
      "report-error" -> text
        .verifying("error.common.problem_report.action_mandatory", error => !error.isEmpty)
        .verifying("error.common.comments_too_long", error => error.size <= 1000),
      "isJavascript" -> boolean,
      "service" -> optional(text),
      "referrer" -> optional(text)
    )(ProblemReport.apply)(ProblemReport.unapply)
  )

  //TODO default to true (or even remove the secure query string) once everyone is off play-frontend so that we use the CSRF check (needs play-partials 1.3.0 and above in every frontend)
  def reportForm(secure: Option[Boolean], preferredCsrfToken: Option[String], service: Option[String]) = UnauthorisedAction { implicit request =>
    val isSecure = secure.getOrElse(false)
    val postEndpoint = if(isSecure) config.CFConfig.externalReportProblemSecureUrl else config.CFConfig.externalReportProblemUrl
    val csrfToken = preferredCsrfToken.orElse { if(isSecure) Some("{{csrfToken}}") else None }

    Ok(views.html.partials.error_feedback(postEndpoint, csrfToken, service))
  }

  def reportFormAjax(service: Option[String]) = UnauthorisedAction { implicit request =>
    Ok(views.html.partials.error_feedback_inner(config.CFConfig.externalReportProblemSecureUrl, None, service))
  }

  def reportFormNonJavaScript(service: Option[String]) = UnauthorisedAction { implicit request =>
    Ok(views.html.problem_reports_nonjavascript(config.CFConfig.externalReportProblemSecureUrl, service))
  }

  def submitSecure = submit

  //TODO remove once everyone is off play-frontend as this doesn't have CSRF check
  def submit = Action.async { implicit request =>
    doReport()
  }

  private[controllers] def doReport(thankYouMessage: Option[String] = None, accounts: Option[Accounts] = None)(implicit request: Request[AnyRef]) = {
    form.bindFromRequest.fold(
      error => {
        if (!error.data.getOrElse("isJavascript", "true").toBoolean) {
          Future.successful(Ok(views.html.problem_reports_error_nonjavascript()))
        } else {
          Future.successful(BadRequest(Json.toJson(Map("status" -> "ERROR"))))
        }
      },
      problemReport => {
        val referrer = if(problemReport.referrer.exists(_.trim.length > 0)) problemReport.referrer.get else referrerFrom(request)
        (for {
          maybeUserAccounts <- accounts.fold(ifEmpty = maybeAuthenticatedUserAccounts)(_ => Future.successful(accounts))
          ticketId <- createTicket(problemReport, request, maybeUserAccounts, referrer)
        } yield {
          if (!problemReport.isJavascript) Ok(views.html.problem_reports_confirmation_nonjavascript(ticketId.ticket_id.toString, thankYouMessage))
          else Ok(Json.toJson(Map("status" -> "OK", "message" -> views.html.ticket_created_body(ticketId.ticket_id.toString, thankYouMessage).toString())))
        }) recover {
          case _ if !problemReport.isJavascript => Ok(views.html.problem_reports_error_nonjavascript())
        }
      }
    )
  }

  private def createTicket(problemReport: ProblemReport, request: Request[AnyRef], accountsOption: Option[Accounts], referrer: String) = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    hmrcDeskproConnector.createDeskProTicket(
      name = problemReport.reportName,
      email = problemReport.reportEmail,
      subject = "Support Request",
      message = problemMessage(problemReport.reportAction, problemReport.reportError),
      referrer = referrer,
      isJavascript = problemReport.isJavascript,
      request = request,
      accountsOption = accountsOption,
      service = problemReport.service
    )
  }

  private[controllers] def problemMessage(action: String, error: String): String = {
    s"""
    ${Messages("problem_report.action")}:
    $action

    ${Messages("problem_report.error")}:
    $error
    """
  }

  private def referrerFrom(request: Request[AnyRef]): String = {
    request.headers.get("referer").getOrElse("/home")
  }
}

case class ProblemReport(reportName: String, reportEmail: String, reportAction: String, reportError: String, isJavascript: Boolean, service: Option[String], referrer: Option[String])


object ProblemReportsController extends ProblemReportsController {
  override lazy val hmrcDeskproConnector = HmrcDeskproConnector
  override lazy val authConnector = FrontendAuthConnector
}
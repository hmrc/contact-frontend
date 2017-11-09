package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import util.DeskproEmailValidator

import scala.concurrent.Future

@Singleton
class ProblemReportsController @Inject()(val hmrcDeskproConnector : HmrcDeskproConnector,
                                         val authConnector : AuthConnector,
                                         emailValidator: DeskproEmailValidator)(implicit  appConfig : AppConfig, override val messagesApi : MessagesApi) extends FrontendController with ContactFrontendActions with I18nSupport {

  private val validateEmail: (String) => Boolean = emailValidator.validate

  val form = Form[ProblemReport](
    mapping(
      "report-name" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70)
        .verifying("error.common.problem_report.name_invalid_characters", name => name.matches( """^[A-Za-z\-.,()'"\s]+$""")),
      "report-email" -> text
        .verifying("error.email", validateEmail)
        .verifying("deskpro.email_too_long", email => email.size <= 255),
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
  def reportForm(secure: Option[Boolean], preferredCsrfToken: Option[String], service: Option[String]) = Action { implicit request =>
    val isSecure = secure.getOrElse(false)
    val postEndpoint = if(isSecure) appConfig.externalReportProblemSecureUrl else appConfig.externalReportProblemUrl
    val csrfToken = preferredCsrfToken.orElse { if(isSecure) Some("{{csrfToken}}") else None }

    Ok(views.html.partials.error_feedback(postEndpoint, csrfToken, service))
  }

  def reportFormAjax(service: Option[String]) = UnauthorisedAction { implicit request =>
    Ok(views.html.partials.error_feedback_inner(appConfig.externalReportProblemSecureUrl, None, service))
  }

  def reportFormNonJavaScript(service: Option[String]) = UnauthorisedAction { implicit request =>
    Ok(views.html.problem_reports_nonjavascript(appConfig.externalReportProblemSecureUrl, service))
  }

  def submitSecure = submit

  //TODO remove once everyone is off play-frontend as this doesn't have CSRF check
  def submit = Action.async { implicit request =>
    doReport()
  }

  private[controllers] def doReport(thankYouMessage: Option[String] = None)(implicit request: Request[AnyRef]) = {
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
          maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
          ticketId <- createTicket(problemReport, request, maybeUserEnrolments, referrer)
        } yield {
          if (!problemReport.isJavascript) Ok(views.html.problem_reports_confirmation_nonjavascript(ticketId.ticket_id.toString, thankYouMessage))
          else Ok(Json.toJson(Map("status" -> "OK", "message" -> views.html.ticket_created_body(ticketId.ticket_id.toString, thankYouMessage).toString())))
        }) recover {
          case _ if !problemReport.isJavascript => Ok(views.html.problem_reports_error_nonjavascript())
        }
      }
    )
  }

  private def createTicket(problemReport: ProblemReport, request: Request[AnyRef], enrolmentsOption: Option[Enrolments], referrer: String) = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    hmrcDeskproConnector.createDeskProTicket(
      name = problemReport.reportName,
      email = problemReport.reportEmail,
      subject = "Support Request",
      message = problemMessage(problemReport.reportAction, problemReport.reportError),
      referrer = referrer,
      isJavascript = problemReport.isJavascript,
      request = request,
      enrolmentsOption = enrolmentsOption,
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

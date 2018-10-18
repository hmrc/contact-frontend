package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import javax.inject.{Inject, Singleton}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}
import util.{DeskproEmailValidator, GetHelpWithThisPageFeature_B}

import scala.concurrent.Future

object ProblemReportForm {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val validateEmail: (String) => Boolean = emailValidator.validate

  val REPORT_NAME_REGEX = """^[A-Za-z0-9\-\.,'\s]+$"""

  val OLD_REPORT_NAME_REGEX = """^[A-Za-z\-.,()'"\s]+$"""

  def form(implicit request: Request[_], appConfig: AppConfig) = Form[ProblemReport](
    mapping(
      "report-name" -> text
        .verifying(s"error.common.problem_report.name_mandatory${featureAorB}", name => !name.isEmpty)
        .verifying(s"error.common.problem_report.name_too_long${featureAorB}",  name => name.size <= 70)
        .verifying(s"error.common.problem_report.name_valid${featureAorB}",     name => {
          if (appConfig.hasFeature(GetHelpWithThisPageFeature_B)) {
            name.matches(REPORT_NAME_REGEX)
          } else {
            name.matches(OLD_REPORT_NAME_REGEX)
          }
        }),
      "report-email" -> text
        .verifying("error.email", validateEmail)
        .verifying("deskpro.email_too_long", email => email.size <= 255),
      "report-action" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.comments_too_long",               action => action.size <= 1000),
      "report-error" -> text
        .verifying("error.common.problem_report.error_mandatory", error => !error.isEmpty)
        .verifying("error.common.comments_too_long",              error => error.size <= 1000),
      "isJavascript" -> boolean,
      "service" -> optional(text),
      "referer" -> optional(text)
    )(ProblemReport.apply)(ProblemReport.unapply))

  private def featureAorB(implicit request: Request[_], appConfig: AppConfig): String = {
    if (appConfig.hasFeature(GetHelpWithThisPageFeature_B)) {
      ".b"
    } else {
      ""
    }
  }

  def emptyForm(referer: Option[String] = None, service : Option[String])(implicit request: Request[_], appConfig: AppConfig): Form[ProblemReport] =
    ProblemReportForm.form.fill(
      ProblemReport(
        reportName = "",
        reportEmail = "",
        reportAction = "",
        reportError = "",
        isJavascript = false,
        service = service,
        referer = referer.orElse(request.headers.get("Referer").orElse(Some("n/a")))
      )
    )
}

@Singleton
class ProblemReportsController @Inject()(val hmrcDeskproConnector: HmrcDeskproConnector,
                                         val authConnector: AuthConnector)(implicit appConfig: AppConfig, override val messagesApi: MessagesApi) extends FrontendController with ContactFrontendActions with I18nSupport {

  //TODO default to true (or even remove the secure query string) once everyone is off play-frontend so that we use the CSRF check (needs play-partials 1.3.0 and above in every frontend)
  def reportForm(secure: Option[Boolean], preferredCsrfToken: Option[String], service: Option[String]) = Action { implicit request =>
    val isSecure = secure.getOrElse(false)
    val postEndpoint = if (isSecure) appConfig.externalReportProblemSecureUrl else appConfig.externalReportProblemUrl
    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value)

    Ok(views.html.partials.error_feedback(ProblemReportForm.emptyForm(service = service), postEndpoint, csrfToken))
  }

  def reportFormAjax(service: Option[String]) = UnauthorisedAction { implicit request =>
    Ok(reportFormAjaxView(ProblemReportForm.emptyForm(service = service)))
  }

  private def reportFormAjaxView(form : Form[ProblemReport])(implicit request : Request[_]) =
    if (appConfig.hasFeature(GetHelpWithThisPageFeature_B)) {
      views.html.partials.error_feedback_inner_b(form, appConfig.externalReportProblemSecureUrl, play.filters.csrf.CSRF.getToken(request).map(_.value))
    } else {
      views.html.partials.error_feedback_inner(form, appConfig.externalReportProblemSecureUrl, play.filters.csrf.CSRF.getToken(request).map(_.value))
    }

  def reportFormNonJavaScript(service: Option[String]) = UnauthorisedAction { implicit request =>
    Ok(views.html.problem_reports_nonjavascript(ProblemReportForm.emptyForm(service = service), appConfig.externalReportProblemSecureUrl))
  }

  def submitSecure: Action[AnyContent] = submit

  def submit = UnauthorisedAction.async { implicit request =>
    ProblemReportForm.form.bindFromRequest.fold(
      (error: Form[ProblemReport]) => {
        if (!error.data.getOrElse("isJavascript", "true").toBoolean) {
          Future.successful(Ok(views.html.problem_reports_nonjavascript(error, appConfig.externalReportProblemSecureUrl)))
        } else {
          Future.successful(Ok(Json.toJson(Map("status" -> "OK", "message" -> reportFormAjaxView(error).toString()))))
        }
      },
      problemReport => {
        val referer = if (problemReport.referer.exists(_.trim.length > 0)) problemReport.referer.get else refererFrom(request)
        (for {
          maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
          ticketId <- createTicket(problemReport, request, maybeUserEnrolments, referer)
        } yield {
            if (!problemReport.isJavascript) {
              javascriptConfirmationPage(ticketId)
            } else {
              nonJavascriptConfirmationPage(ticketId)
            }
          }) recover {
          case _ if !problemReport.isJavascript => Ok(views.html.problem_reports_error_nonjavascript())
        }
      }
    )
  }

  private def nonJavascriptConfirmationPage(ticketId: TicketId)(implicit request : Request[_]) = {
    val view = if (appConfig.hasFeature(GetHelpWithThisPageFeature_B)) {
      views.html.ticket_created_body_b(ticketId.ticket_id.toString, None).toString()
    } else {
      views.html.ticket_created_body(ticketId.ticket_id.toString, None).toString()
    }
    Ok(Json.toJson(Map("status" -> "OK", "message" -> view)))
  }

  private def javascriptConfirmationPage(ticketId: TicketId)(implicit request : Request[_]) = {
    val view = if (appConfig.hasFeature(GetHelpWithThisPageFeature_B)) {
      views.html.problem_reports_confirmation_nonjavascript_b(ticketId.ticket_id.toString, None)
    } else {
      views.html.problem_reports_confirmation_nonjavascript(ticketId.ticket_id.toString, None)
    }
    Ok(view)
  }

  private def createTicket(problemReport: ProblemReport, request: Request[AnyRef], enrolmentsOption: Option[Enrolments], referer: String) = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    hmrcDeskproConnector.createDeskProTicket(
      name = problemReport.reportName,
      email = problemReport.reportEmail,
      subject = "Support Request",
      message = problemMessage(problemReport.reportAction, problemReport.reportError),
      referrer = referer,
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

  private def refererFrom(request: Request[AnyRef]): String = {
    request.headers.get("referer").getOrElse("/home")
  }
}

case class ProblemReport(reportName: String, reportEmail: String, reportAction: String, reportError: String, isJavascript: Boolean, service: Option[String], referer: Option[String])

/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import javax.inject.{Inject, Singleton}
import model.ProblemReport
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, GetHelpWithThisPageOnlyServerSideValidation}
import views.html.partials.{error_feedback, error_feedback_inner}
import views.html.{ProblemReportsNonjsConfirmationPage, ProblemReportsNonjsErrorPage, ProblemReportsNonjsPage, problem_reports_confirmation_nonjavascript, problem_reports_error_nonjavascript, problem_reports_nonjavascript, ticket_created_body}
import play.api.http.HeaderNames._
import scala.concurrent.{ExecutionContext, Future}

object ProblemReportForm {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val validateEmail: (String) => Boolean    = emailValidator.validate

  val REPORT_NAME_REGEX = """^[A-Za-z0-9\-\.,'\s]+$"""

  val OLD_REPORT_NAME_REGEX = """^[A-Za-z\-.,()'"\s]+$"""

  def resolveServiceFromPost(implicit request: Request[_]): Option[String] = {
    val body = request.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined    => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined =>
        body.asMultipartFormData.get.asFormUrlEncoded
    }

    body.get("service").flatMap(_.headOption)
  }

  def form(implicit request: Request[_]) = Form[ProblemReport](
    mapping(
      "report-name"   -> text
        .verifying(
          "problem_report.name.error.required",
          name => !name.isEmpty
        )
        .verifying(
          "problem_report.name.error.length",
          name => name.size <= 70
        )
        .verifying(
          "problem_report.name.error.valid",
          name => name.matches(OLD_REPORT_NAME_REGEX) || name.isEmpty
        ),
      "report-email"  -> text
        .verifying(
          "problem_report.email.error.required",
          email => !email.isEmpty
        )
        .verifying(
          "problem_report.email.error.valid",
          email => validateEmail(email) || email.isEmpty
        )
        .verifying("problem_report.email.error.length", email => email.length <= 255),
      "report-action" -> text
        .verifying(
          s"problem_report.action.error.required",
          action => !action.isEmpty
        )
        .verifying(
          "problem_report.action.error.length",
          action => action.size <= 1000
        ),
      "report-error"  -> text
        .verifying(
          s"problem_report.error.error.required",
          error => !error.isEmpty
        )
        .verifying(
          "problem_report.error.error.length",
          error => error.size <= 1000
        ),
      "isJavascript"  -> boolean,
      "service"       -> optional(text),
      "referrer"      -> optional(text),
      "userAction"    -> optional(text)
    )(ProblemReport.apply)(ProblemReport.unapply)
  )

  def emptyForm(
    service: Option[String]
  )(implicit request: Request[_], appConfig: AppConfig): Form[ProblemReport] =
    ProblemReportForm.form.fill(
      ProblemReport(
        reportName = "",
        reportEmail = "",
        reportAction = "",
        reportError = "",
        isJavascript = false,
        service = service,
        referrer = request.headers.get(REFERER),
        userAction = None
      )
    )
}

@Singleton
class ProblemReportsController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  val authConnector: AuthConnector,
  mcc: MessagesControllerComponents,
  assetsFrontendProblemReportsPage: problem_reports_nonjavascript,
  playFrontendProblemReportsPage: ProblemReportsNonjsPage,
  assetsFrontendProblemReportsErrorPage: problem_reports_error_nonjavascript,
  playFrontendProblemReportsErrorPage: ProblemReportsNonjsErrorPage,
  assetsFrontendProblemReportsConfirmationPage: problem_reports_confirmation_nonjavascript,
  playFrontendProblemReportsConfirmationPage: ProblemReportsNonjsConfirmationPage,
  errorFeedbackForm: error_feedback,
  errorFeedbackFormInner: error_feedback_inner,
  ticketCreatedBody: ticket_created_body
)(implicit appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with ContactFrontendActions
    with DeskproSubmission
    with I18nSupport {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  //TODO default to true (or even remove the secure query string) once everyone is off play-frontend so that we use the CSRF check (needs play-partials 1.3.0 and above in every frontend)
  def reportForm(secure: Option[Boolean], preferredCsrfToken: Option[String], service: Option[String]) = Action {
    implicit request =>
      val isSecure     = secure.getOrElse(false)
      val postEndpoint = if (isSecure) appConfig.externalReportProblemSecureUrl else appConfig.externalReportProblemUrl
      val csrfToken    = preferredCsrfToken.orElse(play.filters.csrf.CSRF.getToken(request).map(_.value))

      Ok(errorFeedbackForm(ProblemReportForm.emptyForm(service = service), postEndpoint, csrfToken, service, None))
  }

  def reportFormAjax(service: Option[String]) = Action { implicit request =>
    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value)
    val referrer  = request.headers.get(REFERER)
    Ok(reportFormAjaxView(ProblemReportForm.emptyForm(service = service), service, csrfToken, referrer))
  }

  private def reportFormAjaxView(
    form: Form[ProblemReport],
    service: Option[String],
    csrfToken: Option[String],
    referrer: Option[String]
  )(implicit request: Request[_]) =
    errorFeedbackFormInner(form, appConfig.externalReportProblemSecureUrl, csrfToken, service, referrer)

  def reportFormNonJavaScript(service: Option[String]) = Action { implicit request =>
    val referrer = request.headers.get(REFERER)
    problemReportsPage(ProblemReportForm.emptyForm(service = service), service, referrer)
  }

  def submitSecure: Action[AnyContent] = submit

  def submit = Action.async { implicit request =>
    val isAjax = request.headers.get("X-Requested-With").contains("XMLHttpRequest")

    ProblemReportForm.form.bindFromRequest.fold(
      (formWithErrors: Form[ProblemReport]) => {

        val referrer = formWithErrors.data.get("referrer").filter(_.trim.nonEmpty).orElse(request.headers.get(REFERER))

        if (isAjax) {
          if (appConfig.hasFeature(GetHelpWithThisPageOnlyServerSideValidation, formWithErrors.data.get("service"))) {
            val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value)
            Future.successful(
              Ok(
                Json.toJson(
                  Map(
                    "status"  -> "OK",
                    "message" -> reportFormAjaxView(formWithErrors, formWithErrors.data.get("service"), csrfToken, referrer).toString()
                  )
                )
              )
            )
          } else {
            Future.successful(BadRequest(Json.toJson(Map("status" -> "ERROR"))))
          }
        } else {
          Future.successful(
            problemReportsPage(formWithErrors, formWithErrors.data.get("service"), referrer)(request)
          )
        }
      },
      problemReport => {

        val referrer = problemReport.referrer.filter(_.trim.nonEmpty).orElse(request.headers.get(REFERER))

        (for {
          maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
          ticketId            <- createProblemReportsTicket(problemReport, request, maybeUserEnrolments, referrer)
        } yield
          if (isAjax) {
            javascriptConfirmationPage(ticketId, problemReport.service)
          } else {
            reportFormNonjsConfirmationPage(ticketId)
          }) recover {
          case _ if !isAjax =>
            val view =
              if (appConfig.enablePlayFrontendProblemReportNonjsForm)
                playFrontendProblemReportsErrorPage()
              else
                assetsFrontendProblemReportsErrorPage()
            Ok(view)
        }
      }
    )
  }

  private def javascriptConfirmationPage(ticketId: TicketId, service: Option[String])(implicit request: Request[_]) = {
    val view = ticketCreatedBody(ticketId.ticket_id.toString, None).toString()
    Ok(Json.toJson(Map("status" -> "OK", "message" -> view)))
  }

  private def problemReportsPage(form: Form[ProblemReport], service: Option[String], referrer: Option[String])(implicit
    request: Request[_]
  ): Result = {
    val view = if (appConfig.enablePlayFrontendProblemReportNonjsForm) {
      playFrontendProblemReportsPage(
        form,
        routes.ProblemReportsController.submit()
      )
    } else {
      assetsFrontendProblemReportsPage(
        form,
        appConfig.externalReportProblemSecureUrl,
        service,
        referrer
      )
    }
    Ok(view)
  }

  private def reportFormNonjsConfirmationPage(ticketId: TicketId)(implicit
    request: Request[_]
  ) = {
    val view = if (appConfig.enablePlayFrontendProblemReportNonjsForm) {
      playFrontendProblemReportsConfirmationPage()
    } else {
      assetsFrontendProblemReportsConfirmationPage(
        ticketId.ticket_id.toString,
        None
      )
    }
    Ok(view)
  }

}

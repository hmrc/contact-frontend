/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector

import javax.inject.{Inject, Singleton}
import model.ReportProblemForm
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Request}
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.DeskproEmailValidator
import views.html.partials.{error_feedback, error_feedback_inner, ticket_created_body}
import views.html.{InternalErrorPage, ReportProblemConfirmationPage, ReportProblemPage}
import play.api.http.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}

object ReportProblemFormBind {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val validateEmail: (String) => Boolean    = emailValidator.validate

  val REPORT_NAME_REGEX = """^[A-Za-z\-.,()'"\s]+$"""

  def resolveServiceFromPost(implicit request: Request[_]): Option[String] = {
    val body = request.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined    => body.asFormUrlEncoded.get
      case body: play.api.mvc.AnyContent if body.asMultipartFormData.isDefined =>
        body.asMultipartFormData.get.asFormUrlEncoded
    }

    body.get("service").flatMap(_.headOption)
  }

  def form: Form[ReportProblemForm] = Form[ReportProblemForm](
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
          name => name.matches(REPORT_NAME_REGEX) || name.isEmpty
        ),
      "report-email"  -> text
        .verifying(
          s"problem_report.email.error.required",
          email => !email.isEmpty
        )
        .verifying(
          s"problem_report.email.error.valid",
          email => validateEmail(email) || email.isEmpty
        )
        .verifying("problem_report.email.error.length", email => email.length <= 255),
      "report-action" -> text
        .verifying(
          s"problem_report.action.error.required",
          action => !action.isEmpty
        )
        .verifying("problem_report.action.error.length", action => action.size <= 1000),
      "report-error"  -> text
        .verifying(
          s"problem_report.error.error.required",
          error => !error.isEmpty
        )
        .verifying("problem_report.error.error.length", error => error.size <= 1000),
      "isJavascript"  -> boolean,
      "service"       -> optional(text),
      "referrer"      -> optional(text),
      "userAction"    -> optional(text)
    )(ReportProblemForm.apply)(ReportProblemForm.unapply)
  )

  def emptyForm(service: Option[String])(implicit request: Request[_]): Form[ReportProblemForm] =
    ReportProblemFormBind.form.fill(
      ReportProblemForm(
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
class ReportProblemController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  val authConnector: AuthConnector,
  mcc: MessagesControllerComponents,
  reportProblemPage: ReportProblemPage,
  confirmationPage: ReportProblemConfirmationPage,
  errorFeedbackForm: error_feedback,
  errorFeedbackFormInner: error_feedback_inner,
  ticketCreatedBody: ticket_created_body,
  errorPage: InternalErrorPage
)(implicit appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with ContactFrontendActions
    with DeskproSubmission
    with I18nSupport {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  def index(service: Option[String]) = Action { implicit request =>
    Ok(page(ReportProblemFormBind.emptyForm(service = service), service))
  }

  def partialIndex(preferredCsrfToken: Option[String], service: Option[String]) = Action { implicit request =>
    val csrfToken = preferredCsrfToken.orElse(play.filters.csrf.CSRF.getToken(request).map(_.value))
    Ok(
      errorFeedbackForm(
        ReportProblemFormBind.emptyForm(service = service),
        appConfig.externalReportProblemUrl,
        csrfToken,
        service,
        None
      )
    )
  }

  def partialAjaxIndex(service: Option[String]) = Action { implicit request =>
    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value)
    val referrer  = request.headers.get(REFERER)
    val form      = ReportProblemFormBind.emptyForm(service = service)
    val view      = errorFeedbackFormInner(form, appConfig.externalReportProblemUrl, csrfToken, service, referrer)
    Ok(view)
  }

  def submit(service: Option[String]) = Action.async { implicit request =>
    doSubmit(service)
  }

  def submitDeprecated(service: Option[String]) = Action.async { implicit request =>
    if (request.headers.get("X-Requested-With").contains("XMLHttpRequest")) doSubmitPartial
    else doSubmit(service)
  }

  private def doSubmit(service: Option[String])(implicit request: MessagesRequest[AnyContent]) =
    ReportProblemFormBind.form.bindFromRequest.fold(
      formWithError => {
        val serviceFromForm = formWithError.data.get("service").flatMap(s => if (s.isEmpty) None else Some(s))
        Future.successful(BadRequest(page(formWithError, service.orElse(serviceFromForm))))
      },
      problemReport => {
        val referrer = problemReport.referrer.filter(_.trim.nonEmpty).orElse(request.headers.get(REFERER))
        (for {
          maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
          _                   <- createProblemReportsTicket(problemReport, request, maybeUserEnrolments, referrer)
        } yield Redirect(routes.ReportProblemController.thanks())) recover { case _ =>
          InternalServerError(errorPage())
        }
      }
    )

  private def doSubmitPartial(implicit request: MessagesRequest[AnyContent]) =
    ReportProblemFormBind.form.bindFromRequest.fold(
      formWithError => Future.successful(BadRequest(Json.toJson(Map("status" -> "ERROR")))),
      problemReport => {
        val referrer = problemReport.referrer.filter(_.trim.nonEmpty).orElse(request.headers.get(REFERER))
        (for {
          maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
          ticketId            <- createProblemReportsTicket(problemReport, request, maybeUserEnrolments, referrer)
        } yield {
          val view = ticketCreatedBody(ticketId.ticket_id.toString, None).toString()
          Ok(Json.toJson(Map("status" -> "OK", "message" -> view)))
        }) recover { case _ =>
          InternalServerError(Json.toJson(Map("status" -> "ERROR")))
        }
      }
    )

  private def page(form: Form[ReportProblemForm], service: Option[String])(implicit
    request: Request[_]
  ) = reportProblemPage(form, routes.ReportProblemController.submit(service))

  def thanks(): Action[AnyContent] = Action { implicit request =>
    Ok(confirmationPage())
  }

}

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
import connectors.enrolments.EnrolmentsConnector

import javax.inject.{Inject, Singleton}
import model.ReportProblemForm
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Request}
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator, RefererHeaderRetriever}
import views.html.partials.{error_feedback, error_feedback_inner, ticket_created_body}
import views.html.{InternalErrorPage, ReportProblemConfirmationPage, ReportProblemPage}

import scala.concurrent.{ExecutionContext, Future}

object ReportProblemFormBind {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val nameValidator                         = NameValidator()

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
          name => name.nonEmpty
        )
        .verifying(
          "problem_report.name.error.length",
          name => name.length <= 70
        )
        .verifying(
          "forms.name.error.invalid",
          name => nameValidator.validate(name) || name.isEmpty
        ),
      "report-email"  -> text
        .verifying(
          s"problem_report.email.error.required",
          email => email.nonEmpty
        )
        .verifying(
          s"problem_report.email.error.valid",
          email => emailValidator.validate(email) || email.isEmpty
        )
        .verifying("problem_report.email.error.length", email => email.length <= 255),
      "report-action" -> text
        .verifying(
          s"problem_report.action.error.required",
          action => action.nonEmpty
        )
        .verifying("problem_report.action.error.length", action => action.length <= 1000),
      "report-error"  -> text
        .verifying(
          s"problem_report.error.error.required",
          error => error.nonEmpty
        )
        .verifying("problem_report.error.error.length", error => error.length <= 1000),
      "isJavascript"  -> boolean,
      "service"       -> optional(text),
      "referrer"      -> optional(text),
      "csrfToken"     -> text,
      "userAction"    -> optional(text)
    )(ReportProblemForm.apply)(ReportProblemForm.unapply)
  )

  def emptyForm(csrfToken: String, service: Option[String], referrer: Option[String]): Form[ReportProblemForm] =
    ReportProblemFormBind.form.fill(
      ReportProblemForm(
        reportName = "",
        reportEmail = "",
        reportAction = "",
        reportError = "",
        isJavascript = false,
        service = service,
        referrer = referrer,
        csrfToken = csrfToken,
        userAction = None
      )
    )
}

@Singleton
class ReportProblemController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  enrolmentsConnector: EnrolmentsConnector,
  mcc: MessagesControllerComponents,
  reportProblemPage: ReportProblemPage,
  confirmationPage: ReportProblemConfirmationPage,
  errorFeedbackForm: error_feedback,
  errorFeedbackFormInner: error_feedback_inner,
  ticketCreatedBody: ticket_created_body,
  errorPage: InternalErrorPage,
  headerRetriever: RefererHeaderRetriever
)(implicit appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  def index(service: Option[String], referrerUrl: Option[String]) = Action { implicit request =>
    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value).getOrElse("")
    val referrer  = referrerUrl orElse headerRetriever.refererFromHeaders
    Ok(page(ReportProblemFormBind.emptyForm(csrfToken, service, referrer), service, referrerUrl))
  }

  def indexDeprecated(service: Option[String], referrerUrl: Option[String]) = Action { implicit request =>
    val referrer = referrerUrl orElse headerRetriever.refererFromHeaders
    Redirect(routes.ReportProblemController.index(service, referrer))
  }

  def partialIndex(preferredCsrfToken: Option[String], service: Option[String]) = Action { implicit request =>
    val csrfToken = preferredCsrfToken.orElse(play.filters.csrf.CSRF.getToken(request).map(_.value))
    val referrer  = headerRetriever.refererFromHeaders
    Ok(
      errorFeedbackForm(
        form = ReportProblemFormBind.emptyForm(csrfToken.getOrElse(""), service, referrer),
        actionUrl = appConfig.externalReportProblemUrl,
        csrfToken = csrfToken,
        service = service,
        referrer = None
      )
    )
  }

  def partialAjaxIndex(service: Option[String]) = Action { implicit request =>
    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value)
    val referrer  = headerRetriever.refererFromHeaders
    val form      = ReportProblemFormBind.emptyForm(csrfToken.getOrElse(""), service, referrer)
    val view      = errorFeedbackFormInner(form, appConfig.externalReportProblemUrl, csrfToken, service, referrer)
    Ok(view)
  }

  def submit(service: Option[String], referrerUrl: Option[String]) = Action.async { implicit request =>
    doSubmit(service, referrerUrl)
  }

  def submitDeprecated(service: Option[String]) = Action.async { implicit request =>
    if (request.headers.get("X-Requested-With").contains("XMLHttpRequest")) doSubmitPartial
    else doSubmit(service, None)
  }

  private def doSubmit(service: Option[String], referrerUrl: Option[String])(implicit
    request: MessagesRequest[AnyContent]
  ) =
    ReportProblemFormBind.form.bindFromRequest.fold(
      formWithError =>
        Future.successful(
          BadRequest(
            page(
              formWithError,
              service.orElse(fromForm("service", formWithError)),
              referrerUrl.orElse(fromForm("referrer", formWithError))
            )
          )
        ),
      problemReport => {
        val referrer = referrerUrl
          .orElse(problemReport.referrer.filter(_.trim.nonEmpty))
          .orElse(headerRetriever.refererFromHeaders)
        (for {
          maybeUserEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
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
        val referrer = problemReport.referrer.filter(_.trim.nonEmpty) orElse headerRetriever.refererFromHeaders
        (for {
          maybeUserEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
          ticketId            <- createProblemReportsTicket(problemReport, request, maybeUserEnrolments, referrer)
        } yield {
          val view = ticketCreatedBody(ticketId.ticket_id.toString, None).toString()
          Ok(Json.toJson(Map("status" -> "OK", "message" -> view)))
        }) recover { case _ =>
          InternalServerError(Json.toJson(Map("status" -> "ERROR")))
        }
      }
    )

  private def page(form: Form[ReportProblemForm], service: Option[String], referrerUrl: Option[String])(implicit
    request: Request[_]
  ) = reportProblemPage(form, routes.ReportProblemController.submit(service, referrerUrl))

  private def fromForm(key: String, form: Form[ReportProblemForm]): Option[String] =
    form.data.get(key).flatMap(r => if (r.isEmpty) None else Some(r))

  def thanks(): Action[AnyContent] = Action { implicit request =>
    Ok(confirmationPage())
  }

}

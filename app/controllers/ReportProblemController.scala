/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.enrolments.EnrolmentsConnector
import model.FormBindings._
import model.ReportProblemForm
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc._
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator, RefererHeaderRetriever, ReferrerUrlChecking}
import views.html.{InternalErrorPage, ReportProblemConfirmationPage, ReportProblemPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ReportProblemFormBind {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val nameValidator                         = NameValidator()

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
      "referrer"      -> optionalRedirectUrlMapping,
      "csrfToken"     -> text,
      "userAction"    -> optional(text)
    )(ReportProblemForm.apply)(ReportProblemForm.unapply)
  )

  def emptyForm(csrfToken: String, service: Option[String], referrer: Option[RedirectUrl]): Form[ReportProblemForm] =
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
  val ticketQueueConnector: DeskproTicketQueueConnector,
  enrolmentsConnector: EnrolmentsConnector,
  mcc: MessagesControllerComponents,
  reportProblemPage: ReportProblemPage,
  confirmationPage: ReportProblemConfirmationPage,
  errorPage: InternalErrorPage,
  headerRetriever: RefererHeaderRetriever
)(implicit appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport
    with ReferrerUrlChecking {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  def index(service: Option[String], referrerUrl: Option[RedirectUrl]) = Action { implicit request =>
    val referrer = maybeSafeRedirectUrl(service, referrerUrl, headerRetriever)
    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value).getOrElse("")
    Ok(page(ReportProblemFormBind.emptyForm(csrfToken, service, referrer), service, referrer))
  }

  def indexDeprecated(service: Option[String], referrerUrl: Option[RedirectUrl]) = Action { implicit request =>
    val referrer = maybeSafeRedirectUrl(service, referrerUrl, headerRetriever)
    Redirect(routes.ReportProblemController.index(service, referrer))
  }

  def submit(service: Option[String], referrerUrl: Option[RedirectUrl]) = Action.async { implicit request =>
    val referrer = maybeSafeRedirectUrl(service, referrerUrl, headerRetriever)
    doSubmit(service, referrer)
  }

  private def doSubmit(service: Option[String], referrerUrl: Option[RedirectUrl])(implicit
    request: MessagesRequest[AnyContent]
  ) = {
    ReportProblemFormBind.form
      .bindFromRequest()
      .fold(
        formWithError =>
          Future.successful(
            BadRequest(
              page(
                formWithError,
                service.orElse(fromForm("service", formWithError)),
                referrerUrl
//                referrerUrl.orElse(fromForm("referrer", formWithError))
              )
            )
          ),
        problemReport => {

//          val referrer = referrerUrl
//            .orElse(problemReport.referrer)
//            .orElse(headerRetriever.refererFromHeaders)
          println("referrerUrl",referrerUrl)
          println("problemReport.referrer",problemReport.referrer)
          println("headerRetriever.refererFromHeaders",headerRetriever.refererFromHeaders)
          (for {
            maybeUserEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
            _                   <- createProblemReportsTicket(problemReport, request, maybeUserEnrolments, referrerUrl)
          } yield Redirect(routes.ReportProblemController.thanks)) recover { case _ =>
            InternalServerError(errorPage())
          }
        }
      )
  }

  private def page(form: Form[ReportProblemForm], service: Option[String], referrerUrl: Option[RedirectUrl])(implicit
    request: Request[_]
  ) = reportProblemPage(
    form,
    routes.ReportProblemController.submit(service, referrerUrl)
  )

  private def fromForm(key: String, form: Form[ReportProblemForm]): Option[String] =
    form.data.get(key).flatMap(r => if (r.isEmpty) None else Some(r))

  def thanks(): Action[AnyContent] = Action { implicit request =>
    Ok(confirmationPage())
  }

}

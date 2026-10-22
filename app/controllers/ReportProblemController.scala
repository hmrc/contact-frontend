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

import config.{AppConfig, ValidationConstants}
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.enrolments.EnrolmentsConnector
import model.Aliases.ReferrerUrl
import model.ReportProblemForm
import play.api.Logging
import play.api.data.Forms.*
import play.api.data.*
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{AnyContent, *}
import services.DeskproSubmission
import uk.gov.hmrc.hmrcfrontend.config.ServiceNavigationConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator, RefererHeaderRetriever}
import util.ServiceNavigationParamBinder.bindServiceNavigationParam
import views.html.{InternalErrorPage, ReportProblemConfirmationPage, ReportProblemPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object ReportProblemFormProvider {
  import ValidationConstants._

  def apply()(using messages: Messages): Form[ReportProblemForm] = Form[ReportProblemForm](
    mapping(
      "report-name"   -> text
        .verifying(
          "problem_report.name.error.required",
          name => name.nonEmpty
        )
        .verifying(
          messages("problem_report.name.error.length", maxNameLength.toString),
          name => name.length <= maxNameLength
        )
        .verifying(
          "forms.name.error.invalid",
          name => NameValidator.validate(name) || name.isEmpty
        ),
      "report-email"  -> text
        .verifying(
          s"problem_report.email.error.required",
          email => email.nonEmpty
        )
        .verifying(
          s"problem_report.email.error.valid",
          // Do not display the regex validation error if already displayed a length error
          email => email.length > maxEmailLength || email.trim.isEmpty || DeskproEmailValidator.validate(email)
        )
        .verifying(
          messages("problem_report.email.error.length", maxEmailLength),
          email => email.length <= maxEmailLength
        ),
      "report-action" -> text
        .verifying(
          s"problem_report.action.error.required",
          action => action.nonEmpty
        )
        .verifying(
          messages("problem_report.action.error.length", maxMultipartTextboxLength.toString),
          action => action.length <= maxMultipartTextboxLength
        ),
      "report-error"  -> text
        .verifying(
          s"problem_report.error.error.required",
          error => error.nonEmpty
        )
        .verifying(
          messages("problem_report.error.error.length", maxMultipartTextboxLength.toString),
          error => error.length <= maxMultipartTextboxLength
        ),
      "isJavascript"  -> boolean,
      "service"       -> optional(text),
      "referrer"      -> optional(text),
      "csrfToken"     -> text,
      "userAction"    -> optional(text)
    )(ReportProblemForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  def emptyForm(csrfToken: String, service: Option[String], referrer: Option[String])(using
    Messages
  ): Form[ReportProblemForm] =
    apply().fill(
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
)(using AppConfig, ExecutionContext, ServiceNavigationConfig)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport
    with Logging {

  given lang(using request: Request[?]): Lang = request.lang

  def index(service: Option[String], referrerUrl: Option[ReferrerUrl]): Action[AnyContent] = Action { request =>
    given Request[AnyContent] = request
    val csrfToken             = play.filters.csrf.CSRF.getToken(request).map(_.value).getOrElse("")
    val referrer              = referrerUrl orElse headerRetriever.refererFromHeaders()
    Ok(page(ReportProblemFormProvider.emptyForm(csrfToken, service, referrer), service, referrerUrl))
  }

  def indexDeprecated(service: Option[String], referrerUrl: Option[ReferrerUrl]): Action[AnyContent] = Action {
    (request: MessagesRequest[AnyContent]) =>
      given MessagesRequest[AnyContent] = request
      val referrer                      = referrerUrl orElse headerRetriever.refererFromHeaders()
      Redirect(routes.ReportProblemController.index(service, referrer).bindServiceNavigationParam)
  }

  def submit(service: Option[String], referrerUrl: Option[ReferrerUrl]): Action[AnyContent] =
    Action.async { request =>
      given MessagesRequest[AnyContent] = request
      doSubmit(service, referrerUrl)
    }

  private def doSubmit(service: Option[String], referrerUrl: Option[String])(using
    request: MessagesRequest[AnyContent]
  ) =
    ReportProblemFormProvider()
      .bindFromRequest()
      .fold(
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
            .orElse(headerRetriever.refererFromHeaders())
          (for {
            maybeUserEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
            _                   <- createProblemReportsTicket(problemReport, request, maybeUserEnrolments, referrer)
          } yield Redirect(routes.ReportProblemController.thanks().bindServiceNavigationParam)) recover {
            case NonFatal(_) =>
              logger.error("Creating problem report ticket failed")
              InternalServerError(errorPage())
          }
        }
      )

  private def page(form: Form[ReportProblemForm], service: Option[String], referrerUrl: Option[String])(using
    Request[?]
  ) = reportProblemPage(form, routes.ReportProblemController.submit(service, referrerUrl).bindServiceNavigationParam)

  private def fromForm(key: String, form: Form[ReportProblemForm]): Option[String] =
    form.data.get(key).flatMap(r => if (r.isEmpty) None else Some(r))

  def thanks(): Action[AnyContent] = Action { request =>
    given MessagesRequest[AnyContent] = request
    Ok(confirmationPage())
  }

}

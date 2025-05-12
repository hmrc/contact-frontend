/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.testOnly

import config.AppConfig
import connectors.deskpro.DeskproTicketQueueConnector
import model.Aliases.ReferrerUrl
import model.ReportOneLoginProblemForm
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Request}
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator, RefererHeaderRetriever}
import views.html.InternalErrorPage
import views.html.testOnly.{ReportOneLoginProblemConfirmationPage, ReportOneLoginProblemPage}

import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ReportOneLoginProblemFormBind {
  private val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  private val nameValidator                         = NameValidator()

  def form: Form[ReportOneLoginProblemForm] = Form[ReportOneLoginProblemForm](
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
    )(ReportOneLoginProblemForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  def emptyForm(csrfToken: String, service: Option[String], referrer: Option[String]): Form[ReportOneLoginProblemForm] =
    ReportOneLoginProblemFormBind.form.fill(
      ReportOneLoginProblemForm(
        name = "",
        nino = "",
        saUtr = None,
        dateOfBirth = new Date(System.currentTimeMillis()),
        email = "",
        phoneNumber = None,
        address = "",
        contactPreference = None,
        complaint = None,
//        service = Some("one-login-complaint"),
        csrfToken = csrfToken
      )
    )
}

@Singleton
class ReportOneLoginProblemController @Inject() (
  val ticketQueueConnector: DeskproTicketQueueConnector,
  mcc: MessagesControllerComponents,
  reportOneLoginProblemPage: ReportOneLoginProblemPage,
  oneLoginConfirmationPage: ReportOneLoginProblemConfirmationPage,
  errorPage: InternalErrorPage,
  headerRetriever: RefererHeaderRetriever
)(using AppConfig, ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport {

  def index(referrerUrl: Option[ReferrerUrl]): Action[AnyContent] = Action { request =>
    given Request[AnyContent] = request

    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value).getOrElse("")
    val referrer  = referrerUrl orElse headerRetriever.refererFromHeaders()

    Ok(page(ReportOneLoginProblemFormBind.emptyForm(csrfToken, Some("one-login-complaint"), referrer), referrerUrl))
  }

  def submit(referrerUrl: Option[ReferrerUrl]): Action[AnyContent] =
    Action.async { request =>
      given MessagesRequest[AnyContent] = request

      doSubmit(referrerUrl)
    }

  private def doSubmit(referrerUrl: Option[String])(using request: MessagesRequest[AnyContent]) =
    ReportOneLoginProblemFormBind.form
      .bindFromRequest()
      .fold(
        formWithError =>
          Future.successful(
            BadRequest(page(formWithError, referrerUrl.orElse(fromForm("referrer", formWithError))))
          ),
        problemReport => {
          createOneLoginProblemTicket(problemReport, request).map { _ =>
            Redirect(routes.ReportOneLoginProblemController.thanks())
          } recover { case _ =>
            InternalServerError(errorPage())
          }
        }
      )

  private def page(form: Form[ReportProblemForm], referrerUrl: Option[String])(using Request[?]) =
    reportOneLoginProblemPage(form, routes.ReportOneLoginProblemController.submit(referrerUrl))

  private def fromForm(key: String, form: Form[ReportProblemForm]): Option[String] =
    form.data.get(key).flatMap(r => if (r.isEmpty) None else Some(r))

  def thanks(): Action[AnyContent] = Action { request =>
    given MessagesRequest[AnyContent] = request

    Ok(oneLoginConfirmationPage())
  }
}

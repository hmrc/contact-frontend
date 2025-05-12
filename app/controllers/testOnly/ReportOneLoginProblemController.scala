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
import model.ReportOneLoginProblemForm
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Request}
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator}
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
      "name"              -> text
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
      "nino"              -> text,
      "saUtr"             -> optional(text),
      "dateOfBirth"       -> date,
      "email"             -> text
        .verifying(
          s"problem_report.email.error.required",
          email => email.nonEmpty
        )
        .verifying(
          s"problem_report.email.error.valid",
          email => emailValidator.validate(email) || email.isEmpty
        ),
      "phone"             -> optional(text),
      "address"           -> text,
      "contactPreference" -> optional(text),
      "complaint"         -> optional(text),
      "csrfToken"         -> text
    )(ReportOneLoginProblemForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  def emptyForm(csrfToken: String): Form[ReportOneLoginProblemForm] =
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
  errorPage: InternalErrorPage
)(using AppConfig, ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport {

  def index(): Action[AnyContent] = Action { request =>
    given Request[AnyContent] = request

    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value).getOrElse("")

    Ok(page(ReportOneLoginProblemFormBind.emptyForm(csrfToken)))
  }

  def submit(): Action[AnyContent] =
    Action.async { request =>
      given MessagesRequest[AnyContent] = request

      doSubmit()
    }

  private def doSubmit()(using request: MessagesRequest[AnyContent]) =
    ReportOneLoginProblemFormBind.form
      .bindFromRequest()
      .fold(
        formWithError =>
          Future.successful(
            BadRequest(page(formWithError))
          ),
        problemReport =>
          createOneLoginProblemTicket(problemReport, request).map { _ =>
            Redirect(routes.ReportOneLoginProblemController.thanks())
          } recover { case _ =>
            InternalServerError(errorPage())
          }
      )

  private def page(form: Form[ReportOneLoginProblemForm])(using Request[?]) =
    reportOneLoginProblemPage(form, routes.ReportOneLoginProblemController.submit())

  private def fromForm(key: String, form: Form[ReportOneLoginProblemForm]): Option[String] =
    form.data.get(key).flatMap(r => if (r.isEmpty) None else Some(r))

  def thanks(): Action[AnyContent] = Action { request =>
    given MessagesRequest[AnyContent] = request

    Ok(oneLoginConfirmationPage())
  }
}

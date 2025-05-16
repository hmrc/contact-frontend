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

package controllers

import config.AppConfig
import connectors.deskpro.DeskproTicketQueueConnector
import model.{ContactPreference, ContactPreferenceFormatter, DateOfBirth, EmailPreference, ReportOneLoginProblemForm}
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DateOfBirthValidator, DeskproEmailValidator, NameValidator, TaxIdentifierValidator}
import views.html.{InternalErrorPage, ReportOneLoginProblemConfirmationPage, ReportOneLoginProblemPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ReportOneLoginProblemFormBind {
  private val emailValidator         = DeskproEmailValidator()
  private val nameValidator          = NameValidator()
  private val taxIdentifierValidator = TaxIdentifierValidator()
  private val dateOfBirthValidator   = DateOfBirthValidator()

  def form: Form[ReportOneLoginProblemForm] = Form[ReportOneLoginProblemForm](
    mapping(
      "name"               -> text
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
      "nino"               -> text
        .verifying(
          "one_login_problem.nino.error",
          nino => nino.nonEmpty
        )
        .verifying(
          "one_login_problem.nino.error",
          nino => taxIdentifierValidator.validateNino(nino) || nino.isEmpty
        ),
      "sa-utr"             -> optional(
        text
          .verifying(
            "one_login_problem.sa-utr.error",
            saUtr => taxIdentifierValidator.validateSaUtr(saUtr)
          )
      ),
      "date-of-birth"      -> mapping(
        "day"   -> text.verifying("one_login_problem.date-of-birth.error.day", day => day.nonEmpty),
        "month" -> text.verifying("one_login_problem.date-of-birth.error.month", month => month.nonEmpty),
        "year"  -> text.verifying("one_login_problem.date-of-birth.error.year", year => year.nonEmpty)
      )(DateOfBirth.apply)(d => Some(Tuple.fromProductTyped(d)))
        .verifying("one_login_problem.date-of-birth.error.invalid", dob => dateOfBirthValidator.isValidDate(dob))
        .verifying(
          "one_login_problem.date-of-birth.error.future",
          dob => dateOfBirthValidator.isNotFutureDate(dob) || !dateOfBirthValidator.isValidDate(dob)
        ),
      "email"              -> text
        .verifying(
          s"problem_report.email.error.required",
          email => email.nonEmpty
        )
        .verifying(
          s"problem_report.email.error.valid",
          email => emailValidator.validate(email) || email.isEmpty
        ),
      "phone-number"       -> optional(text),
      "address"            -> text
        .verifying(
          s"one_login_problem.address.error",
          address => address.nonEmpty
        ),
      "contact-preference" -> of[ContactPreference],
      "complaint"          -> optional(text),
      "csrfToken"          -> text
    )(ReportOneLoginProblemForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  def emptyForm(csrfToken: String): Form[ReportOneLoginProblemForm] =
    ReportOneLoginProblemFormBind.form.fill(
      ReportOneLoginProblemForm(
        name = "",
        nino = "",
        saUtr = None,
        dateOfBirth = DateOfBirth.empty,
        email = "",
        phoneNumber = None,
        address = "",
        contactPreference = EmailPreference,
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
)(using appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport {

  def index(): Action[AnyContent] = Action { request =>
    given Request[AnyContent] = request

    val csrfToken = play.filters.csrf.CSRF.getToken(request).map(_.value).getOrElse("")

    pageIfEnabled(Ok(page(ReportOneLoginProblemFormBind.emptyForm(csrfToken))))
  }

  def submit(): Action[AnyContent] =
    Action.async { request =>
      given MessagesRequest[AnyContent] = request

      pageIfEnabled(doSubmit())
    }

  private def doSubmit()(using request: MessagesRequest[AnyContent]): Future[Result] =
    ReportOneLoginProblemFormBind.form
      .bindFromRequest()
      .fold(
        formWithError =>
          Future.successful(
            BadRequest(page(formWithError))
          ),
        problemReport =>
          createOneLoginProblemTicket(problemReport, request, routes.ReportOneLoginProblemController.index().url).map {
            _ =>
              Redirect(routes.ReportOneLoginProblemController.thanks())
          } recover { case _ =>
            InternalServerError(errorPage())
          }
      )

  private def page(form: Form[ReportOneLoginProblemForm])(using Request[?]) =
    reportOneLoginProblemPage(form, routes.ReportOneLoginProblemController.submit())

  def thanks(): Action[AnyContent] = Action { request =>
    given MessagesRequest[AnyContent] = request

    pageIfEnabled(Ok(oneLoginConfirmationPage()))
  }

  private def pageIfEnabled(resultIfEnabled: Result)(using Request[?]): Result =
    if (appConfig.enableOlfgComplaintsEndpoints) resultIfEnabled
    else NotFound(errorPage())

  private def pageIfEnabled(resultIfEnabled: Future[Result])(using Request[?]): Future[Result] =
    if (appConfig.enableOlfgComplaintsEndpoints) resultIfEnabled
    else Future(NotFound(errorPage()))
}

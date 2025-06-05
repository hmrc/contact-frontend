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

import config.{AppConfig, ContactFrontendErrorHandler}
import connectors.deskpro.DeskproTicketQueueConnector
import model.{ContactPreference, ContactPreferenceFormatter, DateOfBirth, OneLoginComplaintForm}
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator}
import views.html.{InternalErrorPage, OneLoginComplaintConfirmationPage, OneLoginComplaintPage}
import uk.gov.hmrc.domain.Nino

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object OneLoginComplaintFormBind {
  private val emailValidator = DeskproEmailValidator()
  private val nameValidator  = NameValidator()

  def form: Form[OneLoginComplaintForm] = Form[OneLoginComplaintForm](
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
          "one_login_complaint.nino.error",
          nino => nino.nonEmpty
        )
        .verifying(
          "one_login_complaint.nino.error",
          // GOV.UK Design System guidance is to let users enter upper and lower case letters, additional spaces and punctuation
          // https://design-system.service.gov.uk/patterns/national-insurance-numbers/
          nino => Nino.isValid(nino.trim.replaceAll("""\p{Punct}""", "").toUpperCase()) || nino.isEmpty
        ),
      "sa-utr"             -> optional(
        text
          .verifying(
            "one_login_complaint.sa-utr.error",
            saUtr => saUtr.length <= 20
          )
      ),
      "date-of-birth"      -> mapping(
        "day"   -> text.verifying("one_login_complaint.date-of-birth.error.day", day => day.nonEmpty),
        "month" -> text.verifying("one_login_complaint.date-of-birth.error.month", month => month.nonEmpty),
        "year"  -> text.verifying("one_login_complaint.date-of-birth.error.year", year => year.nonEmpty)
      )(DateOfBirth.apply)(d => Some(Tuple.fromProductTyped(d)))
        .verifying("one_login_complaint.date-of-birth.error.invalid", _.isValidDate())
        .verifying(
          "one_login_complaint.date-of-birth.error.future",
          dob => dob.isNotFutureDate() || !dob.isValidDate()
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
      "phone-number"       -> optional(
        text.verifying("one_login_complaint.phone-number.error", phoneNumber => phoneNumber.length <= 50)
      ),
      "address"            -> text
        .verifying(
          s"one_login_complaint.address.error",
          address => address.nonEmpty
        ),
      "contact-preference" -> of[ContactPreference],
      "complaint"          -> text
        .verifying(
          "one_login_complaint.complaint.required",
          complaint => complaint.nonEmpty
        )
        .verifying("one_login_complaint.complaint.error", complaint => complaint.length <= 2000)
    )(OneLoginComplaintForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )
}

@Singleton
class OneLoginComplaintController @Inject() (
  val ticketQueueConnector: DeskproTicketQueueConnector,
  mcc: MessagesControllerComponents,
  oneLoginComplaintPage: OneLoginComplaintPage,
  oneLoginComplaintConfirmationPage: OneLoginComplaintConfirmationPage,
  errorPage: InternalErrorPage,
  errorHandler: ContactFrontendErrorHandler
)(using appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport {

  def index(): Action[AnyContent] = checkIfEnabled {
    Action { request =>
      given Request[AnyContent] = request

      Ok(oneLoginComplaintPage(OneLoginComplaintFormBind.form))
    }
  }

  def submit(): Action[AnyContent] = checkIfEnabled {
    Action.async { request =>
      given MessagesRequest[AnyContent] = request

      doSubmit()
    }
  }

  private def doSubmit()(using request: MessagesRequest[AnyContent]): Future[Result] =
    OneLoginComplaintFormBind.form
      .bindFromRequest()
      .fold(
        formWithError =>
          Future.successful(
            BadRequest(oneLoginComplaintPage(formWithError))
          ),
        oneLoginComplaint =>
          createOneLoginComplaintTicket(
            oneLoginComplaint,
            request,
            appConfig.urlWithPlatformHost(routes.OneLoginComplaintController.index().url())
          )
            .map { _ =>
              Redirect(routes.OneLoginComplaintController.thanks())
            } recover { case _ =>
            InternalServerError(errorPage())
          }
      )

  def thanks(): Action[AnyContent] = checkIfEnabled {
    Action { request =>
      given MessagesRequest[AnyContent] = request

      Ok(oneLoginComplaintConfirmationPage())
    }
  }

  private def checkIfEnabled[A](action: Action[A]): Action[A] =
    Action.async(action.parser) { request =>
      if appConfig.enableOlfgComplaintsEndpoints then action(request)
      else errorHandler.notFoundTemplate(request).map(NotFound(_))
    }
}

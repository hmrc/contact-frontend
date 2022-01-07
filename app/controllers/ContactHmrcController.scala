/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc._
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{DeskproEmailValidator, NameValidator, RefererHeaderRetriever}
import views.html.partials.{contact_hmrc_form, contact_hmrc_form_confirmation}
import views.html.{ContactHmrcConfirmationPage, ContactHmrcPage, InternalErrorPage}

import scala.concurrent.{ExecutionContext, Future}

object ContactHmrcForm {

  private val emailValidator = DeskproEmailValidator()
  private val nameValidator  = NameValidator()

  val form = Form[ContactForm](
    mapping(
      "contact-name"     -> text
        .verifying("contact.name.error.required", name => name.trim.nonEmpty)
        .verifying("forms.name.error.invalid", name => nameValidator.validate(name))
        .verifying("contact.name.error.length", name => name.length <= 70),
      "contact-email"    -> text
        .verifying("contact.email.error.required", email => email.trim.nonEmpty)
        .verifying("contact.email.error.invalid", email => email.trim.isEmpty || emailValidator.validate(email))
        .verifying(
          "contact.email.error.length",
          email => !(email.trim.isEmpty || emailValidator.validate(email)) || email.length <= 255
        ),
      "contact-comments" -> text
        .verifying("contact.comments.error.required", comment => comment.trim.nonEmpty)
        .verifying("contact.comments.error.length", comment => comment.length <= 2000),
      "isJavascript"     -> boolean,
      "referrer"         -> text,
      "csrfToken"        -> text,
      "service"          -> optional(text),
      "userAction"       -> optional(text)
    )(ContactForm.apply)(ContactForm.unapply)
  )
}

@Singleton
class ContactHmrcController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  enrolmentsConnector: EnrolmentsConnector,
  mcc: MessagesControllerComponents,
  contactHmrcForm: contact_hmrc_form,
  contactHmrcFormConfirmation: contact_hmrc_form_confirmation,
  errorPage: InternalErrorPage,
  contactHmrcPage: ContactHmrcPage,
  contactHmrcConfirmationPage: ContactHmrcConfirmationPage,
  headerRetriever: RefererHeaderRetriever
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  def index(service: Option[String], userAction: Option[String], referrerUrl: Option[String]) =
    Action.async { implicit request =>
      Future.successful {
        val referrer  = referrerUrl orElse headerRetriever.refererFromHeaders getOrElse "n/a"
        val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
        val form      = ContactHmrcForm.form.fill(ContactForm(referrer, csrfToken, service, userAction))
        val submit    = routes.ContactHmrcController.submit(service, userAction, referrerUrl)
        val view      = contactHmrcPage(form, submit)
        Ok(view)
      }
    }

  def submit(
    service: Option[String],
    userAction: Option[String],
    referrerUrl: Option[String]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      ContactHmrcForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val submit = routes.ContactHmrcController.submit(service, userAction, referrerUrl)
            Future.successful(BadRequest(contactHmrcPage(formWithErrors, submit)))
          },
          data =>
            (for {
              maybeEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
              _               <- createDeskproTicket(data, maybeEnrolments)
            } yield {
              val thanks = routes.ContactHmrcController.thanks()
              Redirect(thanks)
            }).recover { case _ =>
              InternalServerError(errorPage())
            }
        )
    }

  def thanks = Action.async { implicit request =>
    Future.successful(Ok(contactHmrcConfirmationPage()))
  }

  def partialIndex(submitUrl: String, csrfToken: String, service: Option[String], renderFormOnly: Boolean) =
    Action.async { implicit request =>
      Future.successful {
        Ok(
          contactHmrcForm(
            ContactHmrcForm.form.fill(
              ContactForm(request.headers.get(REFERER).getOrElse("n/a"), csrfToken, service, None)
            ),
            submitUrl,
            renderFormOnly
          )
        )
      }
    }

  def partialSubmit(resubmitUrl: String, renderFormOnly: Boolean) = Action.async { implicit request =>
    ContactHmrcForm.form
      .bindFromRequest()
      .fold(
        error => Future.successful(BadRequest(contactHmrcForm(error, resubmitUrl, renderFormOnly))),
        data =>
          (for {
            enrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
            ticketId   <- createDeskproTicket(data, enrolments)
          } yield Ok(ticketId.ticket_id.toString)).recover { case _ =>
            InternalServerError(errorPage())
          }
      )
  }

  def partialThanks(ticketId: String) = Action { implicit request =>
    Ok(contactHmrcFormConfirmation(ticketId))
  }
}

case class ContactForm(
  contactName: String,
  contactEmail: String,
  contactComments: String,
  isJavascript: Boolean,
  referrer: String,
  csrfToken: String,
  service: Option[String] = Some("unknown"),
  userAction: Option[String] = None
)

object ContactForm {
  def apply(
    referrer: String,
    csrfToken: String,
    service: Option[String],
    userAction: Option[String]
  ): ContactForm =
    ContactForm("", "", "", isJavascript = false, referrer, csrfToken, service, userAction)
}

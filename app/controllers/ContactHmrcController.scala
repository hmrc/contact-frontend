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
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc._
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.DeskproEmailValidator
import views.html.partials.{contact_hmrc_form, contact_hmrc_form_confirmation}
import views.html.{ContactHmrcConfirmationPage, ContactHmrcPage, InternalErrorPage}

import scala.concurrent.{ExecutionContext, Future}

object ContactHmrcForm {

  private val emailValidator = new DeskproEmailValidator()

  val form = Form[ContactForm](
    mapping(
      "contact-name"     -> text
        .verifying("contact.name.error.required", name => name.trim.nonEmpty)
        .verifying("contact.name.error.length", name => name.length <= 70),
      "contact-email"    -> text
        .verifying("contact.email.error.required", email => !email.trim.isEmpty)
        .verifying("contact.email.error.invalid", email => email.trim.isEmpty || emailValidator.validate(email))
        .verifying(
          "contact.email.error.length",
          email => !(email.trim.isEmpty || emailValidator.validate(email)) || email.length <= 255
        ),
      "contact-comments" -> text
        .verifying("contact.comments.error.required", comment => !comment.trim.isEmpty)
        .verifying("contact.comments.error.length", comment => comment.length <= 2000),
      "isJavascript"     -> boolean,
      "referrer"         -> text,
      "csrfToken"        -> text,
      "service"          -> optional(text),
      "abFeatures"       -> optional(text),
      "userAction"       -> optional(text)
    )(ContactForm.apply)(ContactForm.unapply)
  )
}

@Singleton
class ContactHmrcController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  val authConnector: AuthConnector,
  val configuration: Configuration,
  mcc: MessagesControllerComponents,
  contactHmrcForm: contact_hmrc_form,
  contactHmrcFormConfirmation: contact_hmrc_form_confirmation,
  errorPage: InternalErrorPage,
  playFrontendContactPage: ContactHmrcPage,
  playFrontendContactConfirmationPage: ContactHmrcConfirmationPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with AuthorisedFunctions
    with LoginRedirection
    with I18nSupport
    with ContactFrontendActions {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  def index = Action.async { implicit request =>
    loginRedirection(routes.ContactHmrcController.index().url)(authorised(AuthProviders(GovernmentGateway)) {
      Future.successful {
        val referrer  = request.headers.get(REFERER).getOrElse("n/a")
        val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
        val form      = ContactHmrcForm.form.fill(ContactForm(referrer, csrfToken, None, None, None))
        val view      = contactPage(form, true, routes.ContactHmrcController.submit())
        Ok(view)
      }
    })
  }

  def indexUnauthenticated(service: Option[String], userAction: Option[String], referrerUrl: Option[String]) =
    Action.async { implicit request =>
      Future.successful {
        val httpReferrer = request.headers.get(REFERER)
        val referrer     = referrerUrl orElse httpReferrer getOrElse "n/a"
        val csrfToken    = CSRF.getToken(request).map(_.value).getOrElse("")
        val form         = ContactHmrcForm.form.fill(ContactForm(referrer, csrfToken, service, None, userAction))
        val action       = routes.ContactHmrcController.submitUnauthenticated(service, userAction, referrerUrl)
        val view         = contactPage(form, false, action)
        Ok(view)
      }
    }

  private def contactPage(form: Form[ContactForm], isLoggedIn: Boolean, submit: Call)(implicit
    request: Request[Any]
  ) =
    playFrontendContactPage(form, submit)

  def submit = Action.async { implicit request =>
    loginRedirection(routes.ContactHmrcController.index().url)(
      authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.allEnrolments) { allEnrolments =>
        handleSubmit(Some(allEnrolments), routes.ContactHmrcController.thanks(), None, None, None)
      }
    )
  }

  def submitUnauthenticated(
    service: Option[String],
    userAction: Option[String],
    referrerUrl: Option[String]
  ): Action[AnyContent] =
    Action.async { implicit request =>
      handleSubmit(None, routes.ContactHmrcController.thanksUnauthenticated(), service, userAction, referrerUrl)
    }

  private def handleSubmit(
    enrolments: Option[Enrolments],
    thanksRoute: Call,
    service: Option[String],
    userAction: Option[String],
    referrerUrl: Option[String]
  )(implicit request: Request[AnyContent]) =
    ContactHmrcForm.form
      .bindFromRequest()(request)
      .fold(
        error => {
          val submitUrl = {
            if (enrolments.isDefined) routes.ContactHmrcController.submit()
            else routes.ContactHmrcController.submitUnauthenticated(service, userAction, referrerUrl)
          }
          Future.successful(BadRequest(contactPage(error, isLoggedIn = enrolments.isDefined, submitUrl)))
        },
        data =>
          createDeskproTicket(data, enrolments)
            .map { ticketId =>
              Redirect(thanksRoute).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
            }
            .recover { case _ =>
              InternalServerError(errorPage())
            }
      )

  def thanks = Action.async { implicit request =>
    loginRedirection(routes.ContactHmrcController.index().url)(authorised(AuthProviders(GovernmentGateway)) {
      doThanks(request)
    })
  }

  def thanksUnauthenticated = Action.async { implicit request =>
    doThanks(request)
  }

  private def doThanks(implicit request: Request[AnyRef]) = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { _ =>
      Ok(playFrontendContactConfirmationPage())
    }
    Future.successful(result)
  }

  def contactHmrcPartialForm(submitUrl: String, csrfToken: String, service: Option[String], renderFormOnly: Boolean) =
    Action.async { implicit request =>
      Future.successful {
        Ok(
          contactHmrcForm(
            ContactHmrcForm.form.fill(
              ContactForm(request.headers.get(REFERER).getOrElse("n/a"), csrfToken, service, None, None)
            ),
            submitUrl,
            renderFormOnly
          )
        )
      }
    }

  def submitContactHmrcPartialForm(resubmitUrl: String, renderFormOnly: Boolean) = Action.async { implicit request =>
    ContactHmrcForm.form
      .bindFromRequest()(request)
      .fold(
        error => Future.successful(BadRequest(contactHmrcForm(error, resubmitUrl, renderFormOnly))),
        data =>
          (for {
            enrolments <- maybeAuthenticatedUserEnrolments()
            ticketId   <- createDeskproTicket(data, enrolments)
          } yield Ok(ticketId.ticket_id.toString)).recover { case _ =>
            InternalServerError(errorPage())
          }
      )
  }

  def contactHmrcPartialFormConfirmation(ticketId: String) = Action { implicit request =>
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
  abFeatures: Option[String] = None,
  userAction: Option[String] = None
)

object ContactForm {
  def apply(
    referrer: String,
    csrfToken: String,
    service: Option[String],
    abFeatures: Option[String],
    userAction: Option[String]
  ): ContactForm =
    ContactForm("", "", "", isJavascript = false, referrer, csrfToken, service, abFeatures, userAction)
}

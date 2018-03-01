package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}
import views.html.deskpro_error

import scala.concurrent.Future

@Singleton
class PartialsController @Inject() (val hmrcDeskproConnector : HmrcDeskproConnector, val authConnector : AuthConnector)(implicit appConfig : AppConfig, override val messagesApi : MessagesApi) extends FrontendController with DeskproSubmission
  with ContactFrontendActions with I18nSupport {

  def contactHmrcForm(submitUrl: String, csrfToken: String, service: Option[String], renderFormOnly: Option[Boolean]) = Action.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.contact_hmrc_form(ContactHmrcForm.form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a"), csrfToken, service)), submitUrl, renderFormOnly))
      }
  }

  def submitContactHmrcForm(resubmitUrl: String, renderFormOnly: Option[Boolean]) = Action.async {
    implicit request =>
      ContactHmrcForm.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.partials.contact_hmrc_form(error, resubmitUrl, renderFormOnly)))
        },
        data => {
          (for {
            enrolments <- maybeAuthenticatedUserEnrolments()
            ticketId <- createDeskproTicket(data, enrolments)
          } yield {
            Ok(ticketId.ticket_id.toString)
          }).recover {
            case _ => InternalServerError(deskpro_error())
          }
        }
      )
  }

  def contactHmrcFormConfirmation(ticketId: String) = UnauthorisedAction {
    implicit request =>
      Ok(views.html.partials.contact_hmrc_form_confirmation(ticketId))
  }

  def feedbackForm(submitUrl: String, csrfToken: String, service: Option[String], referer: Option[String], canOmitComments : Boolean) = Action.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.feedback_form(FeedbackForm.emptyForm(csrfToken, referer, None, canOmitComments = canOmitComments), submitUrl, service, canOmitComments = canOmitComments))
      }
  }

  def submitFeedbackForm(resubmitUrl: String) = Action.async {
    implicit request =>
      val form =  FeedbackFormBind.form.bindFromRequest()(request)
     form.fold(
        error => {
          Future.successful(BadRequest(views.html.partials.feedback_form(error, resubmitUrl, canOmitComments = form("canOmitComments").value.exists(_ == "true"))))
        },
        data => {
          (for {
            enrolments <- maybeAuthenticatedUserEnrolments()
            ticketId <- createDeskproFeedback(data, enrolments)
          } yield {
            Ok(ticketId.ticket_id.toString)
          }).recover {
            case _ => InternalServerError
          }
        }
      )
  }

  def feedbackFormConfirmation(ticketId: String) = UnauthorisedAction {
    implicit request =>
      Ok(views.html.partials.feedback_form_confirmation(ticketId, None))
  }

}


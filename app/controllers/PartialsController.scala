package controllers

import config.FrontendAuthConnector
import connectors.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import views.html.deskpro_error

import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait PartialsController extends FrontendController with DeskproSubmission with ContactFrontendActions {

  def contactHmrcForm(submitUrl: String, csrfToken: String, service: Option[String], renderFormOnly: Option[Boolean]) = UnauthorisedAction.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.contact_hmrc_form(ContactHmrcForm.form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a"), csrfToken, service)), submitUrl, renderFormOnly))
      }
  }

  def submitContactHmrcForm(resubmitUrl: String, renderFormOnly: Option[Boolean]) = UnauthorisedAction.async {
    implicit request =>
      ContactHmrcForm.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.partials.contact_hmrc_form(error, resubmitUrl, renderFormOnly)))
        },
        data => {
          (for {
            accounts <- maybeAuthenticatedUserAccounts()
            ticketId <- createDeskproTicket(data, accounts)
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

  def feedbackForm(submitUrl: String, csrfToken: String, service: Option[String], referer: Option[String]) = UnauthorisedAction.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.feedback_form(FeedbackForm.emptyForm(csrfToken, referer), submitUrl, service))
      }
  }

  def submitFeedbackForm(resubmitUrl: String) = UnauthorisedAction.async {
    implicit request =>
      FeedbackFormBind.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.partials.feedback_form(error, resubmitUrl)))
        },
        data => {
          (for {
            accounts <- maybeAuthenticatedUserAccounts()
            ticketId <- createDeskproFeedback(data, accounts)
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
      Ok(views.html.partials.feedback_form_confirmation(ticketId))
  }

}

object PartialsController extends PartialsController {
  override val hmrcDeskproConnector = HmrcDeskproConnector
  override protected val authConnector = FrontendAuthConnector
}

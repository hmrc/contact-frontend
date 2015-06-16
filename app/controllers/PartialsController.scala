package controllers

import config.FrontendAuthConnector
import connectors.deskpro.HmrcDeskproConnector
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.play.frontend.auth.User
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import views.html.deskpro_error

import scala.concurrent.Future

trait PartialsController extends FrontendController with DeskproSubmission with ContactFrontendActions {

  def contactHmrcForm(submitUrl: String, csrfToken: String) = UnauthorisedAction.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.contact_hmrc_form(ContactHmrcForm.form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a"), csrfToken)),
          submitUrl))
      }
  }

  def submitContactHmrcForm(resubmitUrl: String) = UnauthorisedAction.async {
    implicit request =>
      ContactHmrcForm.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.partials.contact_hmrc_form(error, resubmitUrl)))
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

  def feedbackForm(submitUrl: String, csrfToken: String) = UnauthorisedAction.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.feedback_form(FeedbackForm.emptyForm(csrfToken), submitUrl))
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

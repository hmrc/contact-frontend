package controllers

import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

import scala.concurrent.Future

trait DeskproSubmission {

  private val Subject = "Contact form submission"

  protected def hmrcDeskproConnector: HmrcDeskproConnector

  def createDeskproTicket(data: ContactForm, accounts: Option[Accounts])(implicit request: Request[AnyContent], hc: HeaderCarrier) : Future[TicketId] = {
    hmrcDeskproConnector.createDeskProTicket(
      name = data.contactName,
      email = data.contactEmail,
      subject = Subject,
      message = data.contactComments,
      referrer = data.referer,
      isJavascript = data.isJavascript,
      request = request,
      accountsOption = accounts,
      service = data.service)
  }

  def createDeskproFeedback(data: FeedbackForm, accounts: Option[Accounts])(implicit request: Request[AnyContent], hc: HeaderCarrier) : Future[TicketId] = {
    hmrcDeskproConnector.createFeedback(
      name = data.name,
      email = data.email,
      rating = data.experienceRating,
      subject = "Beta feedback submission",
      message = data.comments,
      referrer = data.referrer,
      isJavascript = data.javascriptEnabled,
      request = request,
      accountsOption = accounts)
  }
}

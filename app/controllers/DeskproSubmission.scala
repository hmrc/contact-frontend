package controllers

import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait DeskproSubmission {

  private val Subject = "Contact form submission"

  protected def hmrcDeskproConnector: HmrcDeskproConnector

  def createDeskproTicket(data: ContactForm, enrolments: Option[Enrolments])(implicit request: Request[AnyContent], hc: HeaderCarrier) : Future[TicketId] = {
    hmrcDeskproConnector.createDeskProTicket(
      name = data.contactName,
      email = data.contactEmail,
      subject = Subject,
      message = data.contactComments,
      referrer = data.referer,
      isJavascript = data.isJavascript,
      request = request,
      enrolmentsOption = enrolments,
      service = data.service)
  }

  def createDeskproFeedback(data: FeedbackForm, enrolments: Option[Enrolments])(implicit request: Request[AnyContent], hc: HeaderCarrier) : Future[TicketId] = {
    hmrcDeskproConnector.createFeedback(
      name = data.name,
      email = data.email,
      rating = data.experienceRating,
      subject = "Beta feedback submission",
      message = data.comments match {
        case "" => "No comment given"
        case comment => comment
      },
      referrer = data.referrer,
      isJavascript = data.javascriptEnabled,
      request = request,
      enrolmentsOption = enrolments,
      service = data.service)
  }
}

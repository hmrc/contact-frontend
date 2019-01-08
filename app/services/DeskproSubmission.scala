package services

import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import controllers.ContactForm
import model.{FeedbackForm, ProblemReport}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

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
      service = data.service,
      abFeatures = data.abFeatures)
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
      service = data.service,
      abFeatures = data.abFeatures)
  }

  def createProblemReportsTicket(problemReport: ProblemReport, request: Request[AnyRef], enrolmentsOption: Option[Enrolments], referrer: Option[String])(implicit messages: Messages): Future[TicketId] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    hmrcDeskproConnector.createDeskProTicket(
      name = problemReport.reportName,
      email = problemReport.reportEmail,
      subject = "Support Request",
      message = problemMessage(problemReport.reportAction, problemReport.reportError),
      referrer = referrer.getOrElse("/home"),
      isJavascript = problemReport.isJavascript,
      request = request,
      enrolmentsOption = enrolmentsOption,
      service = problemReport.service,
      abFeatures = problemReport.abFeatures
    )
  }

  def problemMessage(action: String, error: String)(implicit messages: Messages): String = {
    s"""
    ${Messages("problem_report.action")}:
    $action

    ${Messages("problem_report.error")}:
    $error
    """
  }
}

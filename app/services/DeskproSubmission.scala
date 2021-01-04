/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package services

import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import controllers.ContactForm
import model.{AccessibilityForm, FeedbackForm, ProblemReport}
import org.apache.http.client.utils.URIBuilder
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future
import scala.util.Try

trait DeskproSubmission {

  import DeskproSubmission.replaceReferrerPath

  private val Subject = "Contact form submission"

  protected def hmrcDeskproConnector: HmrcDeskproConnector

  def createDeskproTicket(data: ContactForm, enrolments: Option[Enrolments])(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[TicketId] =
    hmrcDeskproConnector.createDeskProTicket(
      name = data.contactName,
      email = data.contactEmail,
      subject = Subject,
      message = data.contactComments,
      referrer = replaceReferrerPath(data.referrer, data.userAction),
      isJavascript = data.isJavascript,
      request = request,
      enrolmentsOption = enrolments,
      service = data.service,
      abFeatures = data.abFeatures,
      userAction = data.userAction
    )

  def createDeskproFeedback(data: FeedbackForm, enrolments: Option[Enrolments])(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[TicketId] =
    hmrcDeskproConnector.createFeedback(
      name = data.name,
      email = data.email,
      rating = data.experienceRating.getOrElse(""),
      subject = "Beta feedback submission",
      message = data.comments match {
        case ""      => "No comment given"
        case comment => comment
      },
      referrer = data.referrer,
      isJavascript = data.javascriptEnabled,
      request = request,
      enrolmentsOption = enrolments,
      service = data.service,
      abFeatures = data.abFeatures
    )

  def createProblemReportsTicket(
    problemReport: ProblemReport,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    referrer: Option[String]
  )(implicit messages: Messages): Future[TicketId] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    hmrcDeskproConnector.createDeskProTicket(
      name = problemReport.reportName,
      email = problemReport.reportEmail,
      subject = "Support Request",
      message = problemMessage(problemReport.reportAction, problemReport.reportError),
      referrer = replaceReferrerPath(referrer.getOrElse(""), problemReport.userAction),
      isJavascript = problemReport.isJavascript,
      request = request,
      enrolmentsOption = enrolmentsOption,
      service = problemReport.service,
      abFeatures = problemReport.abFeatures,
      userAction = problemReport.userAction
    )
  }

  def problemMessage(action: String, error: String)(implicit messages: Messages): String =
    s"""
    ${Messages("problem_report.action")}:
    $action

    ${Messages("problem_report.error")}:
    $error
    """

  def createAccessibilityTicket(accessibilityForm: AccessibilityForm, enrolments: Option[Enrolments])(implicit
    req: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[TicketId] =
    hmrcDeskproConnector.createDeskProTicket(
      name = accessibilityForm.name,
      email = accessibilityForm.email,
      subject = "Accessibility Problem",
      message = accessibilityForm.problemDescription,
      referrer = replaceReferrerPath(accessibilityForm.referrer, accessibilityForm.userAction),
      isJavascript = accessibilityForm.isJavascript,
      request = req,
      enrolmentsOption = enrolments,
      service = accessibilityForm.service,
      abFeatures = None,
      userAction = accessibilityForm.userAction
    )

}

object DeskproSubmission {

  def replaceReferrerPath(referrer: String, path: Option[String]): String =
    path
      .filter(_.trim.nonEmpty)
      .map(p => buildUri(referrer).setPath(p).build().toASCIIString)
      .getOrElse(referrer)

  private def buildUri(referrer: String): URIBuilder =
    Try(new URIBuilder(referrer)).getOrElse(new URIBuilder())

}

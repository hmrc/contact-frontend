/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.*
import controllers.ContactForm
import model.{AccessibilityForm, FeedbackForm, OneLoginComplaintForm, ReportProblemForm}
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.mdc.Mdc.*

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait DeskproSubmission extends Logging {

  import DeskproSubmission.replaceReferrerPath

  protected def ticketQueueConnector: DeskproTicketQueueConnector

  def createDeskproTicket(data: ContactForm, enrolments: Option[Enrolments])(using request: Request[AnyContent])(using
    HeaderCarrier,
    ExecutionContext
  ): Future[TicketId] =
    ticketQueueConnector
      .createDeskProTicket(
        name = data.contactName,
        email = data.contactEmail,
        message = data.contactComments,
        referrer = replaceReferrerPath(data.referrer, data.userAction),
        isJavascript = data.isJavascript,
        request = request,
        enrolmentsOption = enrolments,
        service = data.service,
        userAction = data.userAction,
        ticketConstants = ContactHmrcTicketConstants
      )
      .map(ticketId => logTicketCreation(ticketId, data.service))

  def createDeskproFeedback(data: FeedbackForm, enrolments: Option[Enrolments])(using request: Request[AnyContent])(
    using
    HeaderCarrier,
    ExecutionContext
  ): Future[TicketId] =
    ticketQueueConnector
      .createFeedback(
        name = data.name,
        email = data.email,
        rating = data.experienceRating.getOrElse(""),
        message = data.comments match {
          case ""      => "No comment given"
          case comment => comment
        },
        referrer = data.referrer,
        isJavascript = data.javascriptEnabled,
        request = request,
        enrolmentsOption = enrolments,
        service = data.service,
        ticketConstants = BetaFeedbackTicketConstants
      )
      .map(ticketId => logTicketCreation(ticketId, data.service))

  def createProblemReportsTicket(
    data: ReportProblemForm,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    referrer: Option[String]
  )(using Messages, ExecutionContext): Future[TicketId] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    ticketQueueConnector
      .createDeskProTicket(
        name = data.reportName,
        email = data.reportEmail,
        message = problemMessage(data.reportAction, data.reportError),
        referrer = replaceReferrerPath(referrer.getOrElse(""), data.userAction),
        isJavascript = data.isJavascript,
        request = request,
        enrolmentsOption = enrolmentsOption,
        service = data.service,
        userAction = data.userAction,
        ticketConstants = ReportTechnicalProblemTicketConstants
      )
      .map(ticketId => logTicketCreation(ticketId, data.service))
  }

  def problemMessage(action: String, error: String)(using Messages): String =
    s"""
    ${Messages("problem_report.action.label")}:
    $action

    ${Messages("problem_report.error.label")}:
    $error
    """

  def createAccessibilityTicket(data: AccessibilityForm, enrolments: Option[Enrolments])(using
    req: Request[AnyContent]
  )(using HeaderCarrier, ExecutionContext): Future[TicketId] =
    ticketQueueConnector
      .createDeskProTicket(
        name = data.name,
        email = data.email,
        message = data.problemDescription,
        referrer = replaceReferrerPath(data.referrer, data.userAction),
        isJavascript = data.isJavascript,
        request = req,
        enrolmentsOption = enrolments,
        service = data.service,
        userAction = data.userAction,
        ticketConstants = AccessibilityProblemTicketConstants
      )
      .map(ticketId => logTicketCreation(ticketId, data.service))

  def createOneLoginComplaintTicket(
    data: OneLoginComplaintForm,
    request: Request[AnyRef],
    referrer: String
  )(using messages: Messages, ec: ExecutionContext): Future[TicketId] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def oneLoginComplaintMessage(): String = {
      val optionalNoneProvided: String = "None provided"

      s"${messages("one_login_complaint.nino.label")}: ${data.nino}\n\n" +
        s"${messages("one_login_complaint.sa-utr.label")}: ${data.saUtr.getOrElse(optionalNoneProvided)}\n\n" +
        s"${messages("one_login_complaint.date-of-birth.label")}: ${data.dateOfBirth.asFormattedDate()}\n\n" +
        s"${messages("one_login_complaint.phone-number.label")}: ${data.phoneNumber.getOrElse(optionalNoneProvided)}\n\n" +
        s"${messages("one_login_complaint.address.label")}:\n" +
        s"${data.address}\n\n" +
        s"${messages("one_login_complaint.contact-preference.label")}: ${data.contactPreference}\n\n" +
        s"${messages("one_login_complaint.complaint.label")}\n" +
        s"${data.complaint}"
    }

    ticketQueueConnector
      .createDeskProTicket(
        name = data.name,
        email = data.email,
        message = oneLoginComplaintMessage(),
        referrer = referrer,
        isJavascript = false,
        request = request,
        enrolmentsOption = None,
        // This service of `one-login-complaint` should not be made dynamic or changed, as it is linked to Deskpro triggers
        service = Some("one-login-complaint"),
        userAction = None,
        ticketConstants = OneLoginComplaintTicketConstants
      )
      .map(ticketId => logTicketCreation(ticketId, Some("one-login-complaint")))
  }

  private def logTicketCreation(ticketId: TicketId, serviceId: Option[String]): TicketId = {
    putMdc(
      Map(
        "service_id"    -> serviceId.getOrElse("-"),
        "ccs_ticket_id" -> ticketId.ticket_id.toString
      )
    )
    logger.info(
      s"deskpro-ticket-queue ticket created successfully css_ticket_id=${ticketId.ticket_id}"
    )
    ticketId
  }
}

object DeskproSubmission {

  def replaceReferrerPath(referrer: String, path: Option[String]): String =
    path
      .filter(_.trim.nonEmpty)
      .map { p =>
        val absolutePath = if (p.startsWith("/")) p else s"/$p"
        buildUri(referrer).resolve(absolutePath).toASCIIString
      }
      .getOrElse(referrer)

  private def buildUri(referrer: String): URI =
    Try(new URI(referrer)).getOrElse(URI.create(""))

}

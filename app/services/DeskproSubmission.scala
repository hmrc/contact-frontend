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
import model.{AccessibilityForm, FeedbackForm, ReportOneLoginProblemForm, ReportProblemForm}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.net.URI
import scala.concurrent.Future
import scala.util.Try

trait DeskproSubmission {

  import DeskproSubmission.replaceReferrerPath

  protected def ticketQueueConnector: DeskproTicketQueueConnector

  def createDeskproTicket(data: ContactForm, enrolments: Option[Enrolments])(using request: Request[AnyContent])(using
    HeaderCarrier
  ): Future[TicketId] =
    ticketQueueConnector.createDeskProTicket(
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

  def createDeskproFeedback(data: FeedbackForm, enrolments: Option[Enrolments])(using request: Request[AnyContent])(
    using HeaderCarrier
  ): Future[TicketId] =
    ticketQueueConnector.createFeedback(
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

  def createProblemReportsTicket(
    problemReport: ReportProblemForm,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    referrer: Option[String]
  )(using Messages): Future[TicketId] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    ticketQueueConnector.createDeskProTicket(
      name = problemReport.reportName,
      email = problemReport.reportEmail,
      message = problemMessage(problemReport.reportAction, problemReport.reportError),
      referrer = replaceReferrerPath(referrer.getOrElse(""), problemReport.userAction),
      isJavascript = problemReport.isJavascript,
      request = request,
      enrolmentsOption = enrolmentsOption,
      service = problemReport.service,
      userAction = problemReport.userAction,
      ticketConstants = ReportTechnicalProblemTicketConstants
    )
  }

  def problemMessage(action: String, error: String)(using Messages): String =
    s"""
    ${Messages("problem_report.action.label")}:
    $action

    ${Messages("problem_report.error.label")}:
    $error
    """

  def createAccessibilityTicket(accessibilityForm: AccessibilityForm, enrolments: Option[Enrolments])(using
    req: Request[AnyContent]
  )(using HeaderCarrier): Future[TicketId] =
    ticketQueueConnector.createDeskProTicket(
      name = accessibilityForm.name,
      email = accessibilityForm.email,
      message = accessibilityForm.problemDescription,
      referrer = replaceReferrerPath(accessibilityForm.referrer, accessibilityForm.userAction),
      isJavascript = accessibilityForm.isJavascript,
      request = req,
      enrolmentsOption = enrolments,
      service = accessibilityForm.service,
      userAction = accessibilityForm.userAction,
      ticketConstants = AccessibilityProblemTicketConstants
    )

  def createOneLoginProblemTicket(
    problemReport: ReportOneLoginProblemForm,
    request: Request[AnyRef]
  )(using Messages): Future[TicketId] = {
    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def oneLoginProblemMessage(): String =
      s"""
      ${Messages("NINO")}:
      ${problemReport.nino}

      ${Messages("SA UTR")}:
      ${problemReport.saUtr.getOrElse("n/a")}

      ${Messages("Date of Birth")}:
      ${problemReport.dateOfBirth}

      ${Messages("Phone Number")}:
      ${problemReport.phoneNumber.getOrElse("n/a")}

      ${Messages("Address")}:
      ${problemReport.address}

      ${Messages("Contact Preference")}:
      ${problemReport.contactPreference.getOrElse("n/a")}

      ${Messages("Complaint")}:
      ${problemReport.complaint.getOrElse("n/a")}
      """

    ticketQueueConnector.createDeskProTicket(
      name = problemReport.name,
      email = problemReport.email,
      message = oneLoginProblemMessage(),
      referrer = "n/a",
      isJavascript = false,
      request = request,
      enrolmentsOption = None,
      service = Some("one-login-complaint"),
      userAction = None,
      ticketConstants = ReportOneLoginProblemTicketConstants
    )
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

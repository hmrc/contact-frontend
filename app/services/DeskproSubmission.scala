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
import connectors.deskpro.domain._
import controllers.ContactForm
import model.{AccessibilityForm, FeedbackForm, ReportProblemForm}
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

  def createDeskproTicket(data: ContactForm, enrolments: Option[Enrolments])(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
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
      formValues = ContactHmrcFormValues
    )

  def createDeskproFeedback(data: FeedbackForm, enrolments: Option[Enrolments])(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
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
      formValues = BetaFeedbackFormValues
    )

  def createProblemReportsTicket(
    problemReport: ReportProblemForm,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    referrer: Option[String]
  )(implicit messages: Messages): Future[TicketId] = {
    implicit val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
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
      formValues = ReportTechnicalProblemFormValues
    )
  }

  def problemMessage(action: String, error: String)(implicit messages: Messages): String =
    s"""
    ${Messages("problem_report.action.label")}:
    $action

    ${Messages("problem_report.error.label")}:
    $error
    """

  def createAccessibilityTicket(accessibilityForm: AccessibilityForm, enrolments: Option[Enrolments])(implicit
    req: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[TicketId] =
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
      formValues = AccessibilityProblemFormValues
    )

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

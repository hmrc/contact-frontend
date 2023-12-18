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

package connectors.deskpro

import config.AppConfig
import connectors.deskpro.domain.{Feedback, Ticket, TicketConstants, TicketId}
import play.api.libs.json.Json

import javax.inject.Inject
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.{ExecutionContext, Future}

class DeskproTicketQueueConnector @Inject() (
  http: HttpClient,
  servicesConfig: ServicesConfig,
  appConfig: AppConfig,
  auditConnector: AuditConnector
)(implicit
  executionContext: ExecutionContext
) {

  private val serviceUrl: String = servicesConfig.baseUrl("deskpro-ticket-queue")

  def createDeskProTicket(
    name: String,
    email: String,
    message: String,
    referrer: String,
    isJavascript: Boolean,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    service: Option[String],
    userAction: Option[String],
    ticketConstants: TicketConstants
  )(implicit hc: HeaderCarrier): Future[TicketId] = {
    val ticket = Ticket
      .create(
        name,
        email,
        ticketConstants.subject,
        message,
        referrer,
        isJavascript,
        hc,
        request,
        enrolmentsOption,
        service,
        userAction
      )
    http
      .POST[Ticket, TicketId](requestUrl("/deskpro/get-help-ticket"), ticket)
      .map { ticketId =>
        if (appConfig.sendExplicitAuditEvents) {
          auditConnector.sendExplicitAudit(ticketConstants.auditType, Json.toJson(ticket))
        }
        ticketId
      }
      .recover { case nf: NotFoundException =>
        throw UpstreamErrorResponse(nf.getMessage, 404, 500)
      }
  }

  def createFeedback(
    name: String,
    email: String,
    rating: String,
    message: String,
    referrer: String,
    isJavascript: Boolean,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    service: Option[String],
    ticketConstants: TicketConstants
  )(implicit hc: HeaderCarrier): Future[TicketId] = {
    val feedback = Feedback.create(
      name,
      email,
      rating,
      ticketConstants.subject,
      message,
      referrer,
      isJavascript,
      hc,
      request,
      enrolmentsOption,
      service
    )
    http
      .POST[Feedback, TicketId](requestUrl("/deskpro/feedback"), feedback)
      .map { ticketId =>
        if (appConfig.sendExplicitAuditEvents) {
          auditConnector.sendExplicitAudit(ticketConstants.auditType, Json.toJson(feedback))
        }
        ticketId
      }
      .recover { case nf: NotFoundException =>
        throw UpstreamErrorResponse(nf.getMessage, 404, 500)
      }
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"

}

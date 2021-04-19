/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.deskpro.domain

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

case class Ticket private (
  name: String,
  email: String,
  subject: String,
  message: String,
  referrer: String,
  javascriptEnabled: String,
  userAgent: String,
  authId: String,
  areaOfTax: String,
  sessionId: String,
  userTaxIdentifiers: Map[String, String],
  service: Option[String],
  userAction: Option[String]
)

object Ticket extends FieldTransformer with Logging {

  implicit val formats = Json.format[Ticket]

  def create(
    name: String,
    email: String,
    subject: String,
    message: String,
    referrer: String,
    isJavascript: Boolean,
    hc: HeaderCarrier,
    request: Request[AnyRef],
    enrolments: Option[Enrolments],
    service: Option[String],
    userAction: Option[String]
  ): Ticket = {
    val ticket = Ticket(
      name.trim,
      email,
      subject,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(request),
      areaOfTaxOf,
      sessionIdFrom(hc),
      userTaxIdentifiersFromEnrolments(enrolments),
      service,
      userAction
    )
    logger.info(s"Creating ticket $ticket")
    ticket
  }
}

object TicketId {
  implicit val formats = Json.format[TicketId]
}

case class TicketId(ticket_id: Int)

case class Feedback(
  name: String,
  email: String,
  subject: String,
  rating: String,
  message: String,
  referrer: String,
  javascriptEnabled: String,
  userAgent: String,
  authId: String,
  areaOfTax: String,
  sessionId: String,
  userTaxIdentifiers: Map[String, String],
  service: Option[String]
)

object Feedback extends FieldTransformer {

  implicit val formats = Json.format[Feedback]

  def create(
    name: String,
    email: String,
    rating: String,
    subject: String,
    message: String,
    referrer: String,
    isJavascript: Boolean,
    hc: HeaderCarrier,
    request: Request[AnyRef],
    enrolments: Option[Enrolments],
    service: Option[String]
  ): Feedback =
    Feedback(
      name.trim,
      email,
      subject,
      rating,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(request),
      areaOfTaxOf,
      sessionIdFrom(hc),
      userTaxIdentifiersFromEnrolments(enrolments),
      service
    )
}

trait FieldTransformer {
  val NA      = "n/a"
  val UNKNOWN = "unknown"

  def sessionIdFrom(hc: HeaderCarrier) = hc.sessionId.map(_.value).getOrElse("n/a")

  def areaOfTaxOf = UNKNOWN

  def userIdFrom(request: Request[AnyRef]): String =
    if (request.session.get(SessionKeys.authToken).isDefined) "AuthenticatedUser"
    else NA

  def userAgentOf(request: Request[AnyRef]) = request.headers.get("User-Agent").getOrElse("n/a")

  def ynValueOf(javascript: Boolean) = if (javascript) "Y" else "N"

  def userTaxIdentifiersFromEnrolments(enrolmentsOption: Option[Enrolments]): Map[String, String] =
    enrolmentsOption map enrolmentsToTaxIdentifiers getOrElse Map.empty

  private def enrolmentsToTaxIdentifiers(enrolments: Enrolments) = enrolments.enrolments
    .flatMap(
      enrolmentToTaxIdentifiers
    )
    .toMap

  private def enrolmentToTaxIdentifiers(enrolment: Enrolment) =
    enrolment match {
      case enrolment if enrolment.key == "IR-PAYE" => extractEmpRef(enrolment).toMap
      case _                                       => nonPayeEnrolmentToTaxIdentifiers(enrolment).toMap
    }

  private def extractEmpRef(enrolment: Enrolment)                            =
    for (
      taxOfficeNumber <- extractIdentifier(enrolment, "TaxOfficeNumber");
      taxOfficeRef    <- extractIdentifier(enrolment, "TaxOfficeReference")
    )
      yield "empRef" -> s"$taxOfficeNumber/$taxOfficeRef"

  private def extractIdentifier(enrolment: Enrolment, identifierKey: String) =
    enrolment.identifiers.find(_.key == identifierKey).map(_.value)

  private def nonPayeEnrolmentToTaxIdentifiers(enrolment: Enrolment) =
    enrolment.identifiers map { (identifier: EnrolmentIdentifier) =>
      enrolmentIdentifierToTaxIdentifier(enrolment, identifier)
    }

  private def enrolmentIdentifierToTaxIdentifier(enrolment: Enrolment, identifier: EnrolmentIdentifier) =
    (enrolment.key, identifier.key, identifier.value) match {
      case ("HMRC-NI", "NINO", value)             => "nino"                          -> value
      case ("IR-SA", "UTR", value)                => "utr"                           -> value
      case ("IR-CT", "UTR", value)                => "ctUtr"                         -> value
      case ("HMCE-VATVAR-ORG", "VATRegNo", value) => "vrn"                           -> value
      case ("HMCE-VATDEC-ORG", "VATRegNo", value) => "vrn"                           -> value
      case (enrolmentKey, identifierKey, value)   => s"$enrolmentKey/$identifierKey" -> value
    }
}

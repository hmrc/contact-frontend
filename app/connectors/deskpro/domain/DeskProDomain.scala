/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package connectors.deskpro.domain

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
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
  userTaxIdentifiers: UserTaxIdentifiers,
  service: Option[String],
  abFeatures: Option[String],
  userAction: Option[String])

object UserTaxIdentifiers {
  implicit val formats = Json.format[UserTaxIdentifiers]
}

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
    abFeatures: Option[String],
    userAction: Option[String]): Ticket = {
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
      abFeatures,
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

case class UserTaxIdentifiers(
  nino: Option[String],
  ctUtr: Option[String],
  utr: Option[String],
  vrn: Option[String],
  empRef: Option[String])

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
  userTaxIdentifiers: UserTaxIdentifiers,
  service: Option[String],
  abFeatures: Option[String])

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
    service: Option[String],
    abFeatures: Option[String]): Feedback =
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
      service,
      abFeatures
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

  private def extractIdentifier(enrolments: Enrolments, enrolment: String, identifierKey: String): Option[String] =
    enrolments.getEnrolment(enrolment).flatMap(_.identifiers.find(_.key == identifierKey)).map(_.value)

  def userTaxIdentifiersFromEnrolments(enrolmentsOption: Option[Enrolments]) =
    enrolmentsOption
      .map { enrolments =>
        val nino  = extractIdentifier(enrolments, "HMRC-NI", "NINO")
        val saUtr = extractIdentifier(enrolments, "IR-SA", "UTR")
        val ctUtr = extractIdentifier(enrolments, "IR-CT", "UTR")
        val vrn = extractIdentifier(enrolments, "HMCE-VATDEC-ORG", "VATRegNo")
          .orElse(extractIdentifier(enrolments, "HMCE-VATVAR-ORG", "VATRegNo"))

        val empRef = for (taxOfficeNumber <- extractIdentifier(enrolments, "IR-PAYE", "TaxOfficeNumber");
                          taxOfficeRef <- extractIdentifier(enrolments, "IR-PAYE", "TaxOfficeReference"))
          yield s"$taxOfficeNumber/$taxOfficeRef"

        UserTaxIdentifiers(nino, ctUtr, saUtr, vrn, empRef)
      }
      .getOrElse(UserTaxIdentifiers(None, None, None, None, None))
}

package connectors.deskpro.domain

import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import play.api.mvc.Request
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.SessionKeys

case class Ticket private(name: String,
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
                          service: Option[String])


object UserTaxIdentifiers {
  implicit val formats = Json.format[UserTaxIdentifiers]
}

object Ticket extends FieldTransformer {

  implicit val formats = Json.format[Ticket]

  def create(name: String,
             email: String,
             subject: String,
             message: String,
             referrer: String,
             isJavascript: Boolean,
             hc: HeaderCarrier,
             request: Request[AnyRef],
             accountsOption: Option[Accounts],
             service: Option[String]): Ticket =
    Ticket(
      name.trim,
      email,
      subject,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(request, hc),
      areaOfTaxOf(request),
      sessionIdFrom(hc),
      userTaxIdentifiersFromAccounts(accountsOption),
      service)
}

object TicketId {
  implicit val formats = Json.format[TicketId]
}

case class TicketId(ticket_id: Int)


case class UserTaxIdentifiers(nino: Option[Nino],
                              ctUtr: Option[CtUtr],
                              utr: Option[SaUtr],
                              vrn: Option[Vrn],
                              empRef: Option[EmpRef])


case class Feedback(name: String,
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
                    userTaxIdentifiers: UserTaxIdentifiers)


object Feedback extends FieldTransformer {

  implicit val formats = Json.format[Feedback]

  def create(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, hc: HeaderCarrier, request: Request[AnyRef], accounts: Option[Accounts]): Feedback =
    Feedback(
      name.trim,
      email,
      subject,
      rating,
      message.trim,
      referrer,
      ynValueOf(isJavascript),
      userAgentOf(request),
      userIdFrom(request, hc),
      areaOfTaxOf(request),
      sessionIdFrom(hc),
      userTaxIdentifiersFromAccounts(accounts))
}


trait FieldTransformer {
  val NA = "n/a"
  val taxCreditRenewals = "taxcreditrenewals"

  def sessionIdFrom(hc: HeaderCarrier) = hc.sessionId.map(_.value).getOrElse("n/a")

  def areaOfTaxOf(request: Request[AnyRef]) = request.session.get(SessionKeys.authProvider) match {
    // TODO IDA in the future will not only be for PAYE so another way will be required to map to area of tax
    case Some("IDA") => "paye"
    case Some("GGW") => "biztax"
    case Some("NTC") => taxCreditRenewals
    case _ => NA
  }

  def userIdFrom(request: Request[AnyRef], hc: HeaderCarrier): String = request.session.get(SessionKeys.sensitiveUserId) match {
    case Some("true") => NA
    case _ => hc.userId.map(_.value).getOrElse(NA)
  }

  def userAgentOf(request: Request[AnyRef]) = request.headers.get("User-Agent").getOrElse("n/a")

  def ynValueOf(javascript: Boolean) = if (javascript) "Y" else "N"

  def userTaxIdentifiersOf(userOption: Option[AuthContext]) = {
    userTaxIdentifiersFromAccounts(userOption.map(_.principal.accounts))
  }

  def userTaxIdentifiersFromAccounts(accountsOption: Option[Accounts]) = {
    accountsOption.map {
      accounts =>
        val nino = accounts.paye.map(paye => paye.nino)
        val saUtr = accounts.sa.map(sa => sa.utr)
        val ctUtr = accounts.ct.map(ct => ct.utr)
        val vrn = accounts.vat.map(vat => vat.vrn)
        val empRef = accounts.epaye.map(epaye => epaye.empRef)
        UserTaxIdentifiers(nino, ctUtr, saUtr, vrn, empRef)
    }.getOrElse(UserTaxIdentifiers(None, None, None, None, None))
  }
}

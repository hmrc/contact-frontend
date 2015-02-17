package connectors.deskpro

import connectors.deskpro.domain.{Feedback, TicketId, Ticket}
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HttpPost
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

object HmrcDeskproConnector extends HmrcDeskproConnector with ServicesConfig {
  override lazy val serviceUrl = baseUrl("hmrc-deskpro")
  override val http = WSHttp
}

trait HmrcDeskproConnector {

  import play.api.libs.json.Json
  import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts
  import uk.gov.hmrc.play.microservice.domain.User

  import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

  def serviceUrl: String

  def http: HttpPost

  def createTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], userOption: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {

    createDeskProTicket(name, email, subject, message, referrer, isJavascript, request, userOption.map(_.userAuthority.accounts))
  }

  def createDeskProTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], accountsOption: Option[Accounts])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    http.POST[Ticket](requestUrl("/deskpro/ticket"), Ticket.create(name, email, subject, message, referrer, isJavascript, hc, request, accountsOption)).map { response =>
      Json.fromJson[TicketId](response.json).asOpt
    }
  }

  def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], userOption: Option[User])(implicit hc: HeaderCarrier): Future[Option[TicketId]] = {
    http.POST[Feedback](requestUrl("/deskpro/feedback"), Feedback.create(name, email, rating, subject, message, referrer, isJavascript, hc, request, userOption)).map { response =>
      Json.fromJson[TicketId](response.json).asOpt
    }
  }


  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"

}

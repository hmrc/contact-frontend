package connectors.deskpro

import config.WSHttp
import connectors.deskpro.domain.{Feedback, Ticket, TicketId}
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HttpPost, NotFoundException, Upstream5xxResponse}

import scala.concurrent.Future

object HmrcDeskproConnector extends HmrcDeskproConnector with ServicesConfig {
  override lazy val serviceUrl = baseUrl("hmrc-deskpro")
  override val http = WSHttp
}

trait HmrcDeskproConnector {

  import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts
  import uk.gov.hmrc.play.frontend.auth.User

  def serviceUrl: String

  def http: HttpPost

  def createTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], userOption: Option[User])(implicit hc: HeaderCarrier): Future[TicketId] = {

    createDeskProTicket(name, email, subject, message, referrer, isJavascript, request, userOption.map(_.userAuthority.accounts))
  }

  def createDeskProTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], accountsOption: Option[Accounts])(implicit hc: HeaderCarrier): Future[TicketId] = {
    http.POST[Ticket, TicketId](requestUrl("/deskpro/ticket"), Ticket.create(name, email, subject, message, referrer, isJavascript, hc, request, accountsOption)) recover {
      case nf: NotFoundException => throw new Upstream5xxResponse(nf.getMessage, 404, 500)
    }
  }

  def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], userOption: Option[User])(implicit hc: HeaderCarrier): Future[TicketId] = {
    http.POST[Feedback, TicketId](requestUrl("/deskpro/feedback"), Feedback.create(name, email, rating, subject, message, referrer, isJavascript, hc, request, userOption)) recover {
      case nf: NotFoundException => throw new Upstream5xxResponse(nf.getMessage, 404, 500)
    }
  }


  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"

}

package connectors.deskpro

import config.WSHttp
import connectors.deskpro.domain.{Feedback, Ticket, TicketId}
import play.api.mvc.Request
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpPost, NotFoundException, Upstream5xxResponse }

object HmrcDeskproConnector extends HmrcDeskproConnector with ServicesConfig {
  override lazy val serviceUrl = baseUrl("hmrc-deskpro")
  override val http = WSHttp
}

trait HmrcDeskproConnector {

  def serviceUrl: String

  def http: HttpPost

  def createDeskProTicket(name: String, email: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], accountsOption: Option[Accounts], service: Option[String])(implicit hc: HeaderCarrier): Future[TicketId] = {
    http.POST[Ticket, TicketId](requestUrl("/deskpro/get-help-ticket"), Ticket.create(name, email, subject, message, referrer, isJavascript, hc, request, accountsOption, service)) recover {
      case nf: NotFoundException => throw new Upstream5xxResponse(nf.getMessage, 404, 500)
    }
  }

  def createFeedback(name: String, email: String, rating: String, subject: String, message: String, referrer: String, isJavascript: Boolean, request: Request[AnyRef], accountsOption: Option[Accounts], service: Option[String])(implicit hc: HeaderCarrier): Future[TicketId] = {
    http.POST[Feedback, TicketId](requestUrl("/deskpro/feedback"), Feedback.create(name, email, rating, subject, message, referrer, isJavascript, hc, request, accountsOption, service)) recover {
      case nf: NotFoundException => throw new Upstream5xxResponse(nf.getMessage, 404, 500)
    }
  }


  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"

}

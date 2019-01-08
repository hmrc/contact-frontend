package connectors.deskpro

import javax.inject.Inject
import connectors.deskpro.domain.{Feedback, Ticket, TicketId}
import play.api.{Configuration, Environment}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class HmrcDeskproConnector @Inject()(http: HttpClient, environment: Environment, configuration: Configuration)
    extends ServicesConfig {

  def serviceUrl: String = baseUrl("hmrc-deskpro")

  def createDeskProTicket(
    name: String,
    email: String,
    subject: String,
    message: String,
    referrer: String,
    isJavascript: Boolean,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    service: Option[String],
    abFeatures: Option[String])(implicit hc: HeaderCarrier): Future[TicketId] = {
    val ticket = Ticket
      .create(name, email, subject, message, referrer, isJavascript, hc, request, enrolmentsOption, service, abFeatures)
    http.POST[Ticket, TicketId](requestUrl("/deskpro/get-help-ticket"), ticket) recover {
      case nf: NotFoundException => throw new Upstream5xxResponse(nf.getMessage, 404, 500)
    }
  }

  def createFeedback(
    name: String,
    email: String,
    rating: String,
    subject: String,
    message: String,
    referrer: String,
    isJavascript: Boolean,
    request: Request[AnyRef],
    enrolmentsOption: Option[Enrolments],
    service: Option[String],
    abFeatures: Option[String])(implicit hc: HeaderCarrier): Future[TicketId] =
    http.POST[Feedback, TicketId](
      requestUrl("/deskpro/feedback"),
      Feedback.create(
        name,
        email,
        rating,
        subject,
        message,
        referrer,
        isJavascript,
        hc,
        request,
        enrolmentsOption,
        service,
        abFeatures)
    ) recover {
      case nf: NotFoundException => throw new Upstream5xxResponse(nf.getMessage, 404, 500)
    }

  private def requestUrl[B, A](uri: String): String = s"$serviceUrl$uri"

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration
}

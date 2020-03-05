package connectors.deskpro

import connectors.deskpro.domain.{Feedback, Ticket, TicketId}
import javax.inject.Inject
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class HmrcDeskproConnector @Inject()(http: HttpClient,
                                     servicesConfig: ServicesConfig)
                                    (implicit executionContext: ExecutionContext) {

  def serviceUrl: String = servicesConfig.baseUrl("hmrc-deskpro")

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
    abFeatures: Option[String],
    userAction: Option[String])(implicit hc: HeaderCarrier): Future[TicketId] = {
    val ticket = Ticket
      .create(name, email, subject, message, referrer, isJavascript, hc, request, enrolmentsOption, service, abFeatures,
        userAction)
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

}

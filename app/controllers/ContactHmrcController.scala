package controllers

import config.FrontendAuthConnector
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Controller, Request}
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.{Actions, User}
import uk.gov.hmrc.play.validators.Validators
import views.html.deskpro_error

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ContactHmrcForm {
  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text
        .verifying("error.common.problem_report.name_mandatory", name => !name.trim.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70),
      "contact-email" -> Validators.emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),
      "contact-comments" -> text
        .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
        .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
      "isJavascript" -> boolean,
      "referer" -> text,
      "csrfToken" -> text
    )(ContactForm.apply)(ContactForm.unapply)
  )
}

trait ContactHmrcController extends Controller with Actions with DeskproSubmission {

  override implicit val authConnector = FrontendAuthConnector

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

  lazy val GovernmentGateway: GovernmentGatewayAuthProvider = {
    new GovernmentGatewayAuthProvider(routes.ContactHmrcController.index().url)
  }

  def index = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async {
    implicit user => implicit request =>
      Future.successful {
        Ok(views.html.contact_hmrc(ContactHmrcForm.form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a"), CSRF.getToken(request).map{ _.value }.getOrElse("")))
        ))
      }
  })

  def submit = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async {
    implicit user => implicit request =>
      ContactHmrcForm.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.contact_hmrc(error)))
        },
        data => {
          val ticketIdF: Future[TicketId] = createDeskproTicket(data, Some(user.userAuthority.accounts))

          ticketIdF.map{ ticketId =>
            Redirect(routes.ContactHmrcController.thanks()).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
          }.recover {
            case _ => InternalServerError(deskpro_error())
          }
        })
  })

  def thanks = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async({
    implicit user => implicit request => doThanks(user, request)
  }))

  def doThanks(implicit user: User, request: Request[AnyRef]) = {

    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.contact_hmrc_confirmation(ticketId))
    }
    Future.successful(result)

  }

}

object ContactHmrcController extends ContactHmrcController {
  override val hmrcDeskproConnector = HmrcDeskproConnector
}

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean, referer: String, csrfToken: String)

object ContactForm {
  def apply(referer: String, csrfToken: String): ContactForm = ContactForm("", "", "", isJavascript = false, referer, csrfToken)
}

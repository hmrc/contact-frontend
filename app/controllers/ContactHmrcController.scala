package controllers

import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import controllers.common.actions.Actions
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Controller, Request}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.AuthConnector
import uk.gov.hmrc.play.microservice.domain.User
import uk.gov.hmrc.play.validators.Validators
import views.html.deskpro_error

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactHmrcController extends Controller with Actions {

  override implicit val authConnector = AuthConnector

  lazy val hmrcDeskproConnector = HmrcDeskproConnector

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

  lazy val GovernmentGateway: GovernmentGatewayAuthProvider = {
    new GovernmentGatewayAuthProvider(routes.ContactHmrcController.index().url)
  }

  val Subject = "Contact form submission"

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
      "referer" -> text
    )(ContactForm.apply)(ContactForm.unapply)
  )

  def index = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async {
    implicit user => implicit request =>
      Future.successful {
        Ok(views.html.contact_hmrc(form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a")))))
      }
  })

  def submit = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway).async {
    implicit user => implicit request =>
      form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.contact_hmrc(error)))
        },
        data => {
          import data._

          val ticketIdF: Future[Option[TicketId]] = hmrcDeskproConnector.createTicket(contactName, contactEmail, Subject, contactComments, referer, data.isJavascript, request, Some(user))

          ticketIdF.map{
            case Some(x:TicketId) => Redirect(routes.ContactHmrcController.thanks()).withSession(request.session + ("ticketId" -> x.ticket_id.toString))
            case None => InternalServerError(deskpro_error())
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

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean, referer: String)

object ContactForm {
  def apply(referer: String): ContactForm = ContactForm("", "", "", isJavascript = false, referer)
}

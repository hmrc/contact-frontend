package controllers

import controllers.common.actions.Actions
import controllers.common.service.Connectors
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Controller, Request}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.play.connectors.HeaderCarrier
import uk.gov.hmrc.play.validators.Validators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactHmrcController extends Controller with Actions {

  override implicit def authConnector: AuthConnector = Connectors.authConnector

  lazy val hmrcDeskproConnector = Connectors.hmrcDeskproConnector

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

          val ticketIdF = hmrcDeskproConnector.createTicket(contactName, contactEmail, Subject, contactComments, referer, data.isJavascript, request, Some(user))

          ticketIdF map {
            case Some(ticketId) => Redirect(routes.ContactHmrcController.thanks()).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
            case None => InternalServerError("Deskpro failure")
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

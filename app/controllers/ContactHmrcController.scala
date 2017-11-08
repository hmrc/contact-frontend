package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext, GovernmentGateway}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.validators.Validators
import views.html.deskpro_error

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
      "csrfToken" -> text,
      "service" -> optional(text)
    )(ContactForm.apply)(ContactForm.unapply)
  )
}

@Singleton
class ContactHmrcController @Inject() (val hmrcDeskproConnector: HmrcDeskproConnector, authProvider : GovernmentGatewayAuthProvider, val authConnector : AuthConnector)(implicit appConfig : AppConfig) extends FrontendController with Actions with DeskproSubmission {

  lazy val GovernmentGateway: GovernmentGateway = authProvider.forUrl(routes.ContactHmrcController.index().url)

  def index = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      Future.successful {
        val referer = request.headers.get("Referer").getOrElse("n/a")
        val csrfToken = CSRF.getToken(request).map{ _.value }.getOrElse("")
        Ok(views.html.contact_hmrc(ContactHmrcForm.form.fill(ContactForm(referer, csrfToken, None))))
      }
  })

  def submit = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>

      ContactHmrcForm.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.contact_hmrc(error)))
        },
        data => {
          val ticketIdF: Future[TicketId] = createDeskproTicket(data, Some(user.principal.accounts))

          ticketIdF.map{ ticketId =>
            Redirect(routes.ContactHmrcController.thanks()).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
          }.recover {
            case _ => InternalServerError(deskpro_error())
          }
        })
  })

  def thanks = WithNewSessionTimeout(AuthenticatedBy(GovernmentGateway, pageVisibility = GGConfidence).async({
    implicit user => implicit request => doThanks(user, request)
  }))

  def doThanks(implicit user: AuthContext, request: Request[AnyRef]) = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.contact_hmrc_confirmation(ticketId))
    }
    Future.successful(result)
  }

}

case class ContactForm(contactName: String, contactEmail: String, contactComments: String, isJavascript: Boolean, referer: String, csrfToken: String, service: Option[String] = Some("unknown"))

object ContactForm {
  def apply(referer: String, csrfToken: String, service: Option[String]): ContactForm = ContactForm("", "", "", isJavascript = false, referer, csrfToken, service)
}

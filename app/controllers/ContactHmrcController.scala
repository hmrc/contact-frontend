package controllers

import javax.inject.{Inject, Singleton}
import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import play.api.{Configuration, Environment}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, Request}
import play.filters.csrf.CSRF
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}
import util.DeskproEmailValidator
import views.html.deskpro_error

import scala.concurrent.Future

object ContactHmrcForm {

  private val emailValidator = new DeskproEmailValidator()
  private val validateEmail: (String) => Boolean = emailValidator.validate

  val form = Form[ContactForm](
    mapping(
      "contact-name" -> text
        .verifying("error.common.problem_report.name_mandatory", name => !name.trim.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70),
      "contact-email" -> text
        .verifying("error.email", validateEmail)
        .verifying("deskpro.email_too_long", email => email.size <= 255),
      "contact-comments" -> text
        .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
        .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
      "isJavascript" -> boolean,
      "referer" -> text,
      "csrfToken" -> text,
      "service" -> optional(text),
      "abFeatures" -> optional(text)
    )(ContactForm.apply)(ContactForm.unapply)
  )
}

@Singleton
class ContactHmrcController @Inject()(val hmrcDeskproConnector: HmrcDeskproConnector, val authConnector : AuthConnector,
                                      val configuration : Configuration, val environment: Environment)(implicit
                                                                                      val appConfig : AppConfig,
                                                                                      override val messagesApi : MessagesApi)
  extends FrontendController with DeskproSubmission with AuthorisedFunctions with LoginRedirection with I18nSupport
    with ContactFrontendActions {

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  def index = Action.async { implicit request =>
    loginRedirection(routes.ContactHmrcController.index().url)(
      authorised(AuthProviders(GovernmentGateway))({
        Future.successful {
          val referer = request.headers.get("Referer").getOrElse("n/a")
          val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
          Ok(views.html.contact_hmrc(ContactHmrcForm.form.fill(ContactForm(referer, csrfToken, None, None)), loggedIn = true))
        }
      }))
  }

  def indexUnauthenticated(service: String) = Action.async { implicit request =>
    Future.successful {
      val referer = request.headers.get("Referer").getOrElse("n/a")
      val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
      Ok(views.html.contact_hmrc(ContactHmrcForm.form.fill(ContactForm(referer, csrfToken, Some(service), None)), loggedIn = false))
    }
  }

  def submit = Action.async { implicit request =>
    loginRedirection(routes.ContactHmrcController.index().url)(authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.allEnrolments) {
      allEnrolments =>
        handleSubmit(Some(allEnrolments), routes.ContactHmrcController.thanks())
    })
  }

  def submitUnauthenticated = Action.async { implicit request =>
    handleSubmit(None, routes.ContactHmrcController.thanksUnauthenticated())
  }

  private def handleSubmit(enrolments: Option[Enrolments], thanksRoute: Call)(
    implicit request: Request[AnyContent]) = {
    ContactHmrcForm.form.bindFromRequest()(request).fold(
      error => {
        Future.successful(BadRequest(views.html.contact_hmrc(error, enrolments.isDefined)))
      },
      data => {
        createDeskproTicket(data, enrolments).map { ticketId =>
          Redirect(thanksRoute).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
        }.recover {
          case _ => InternalServerError(deskpro_error())
        }
      }
    )
  }

  def thanks = Action.async { implicit request =>
    loginRedirection(routes.ContactHmrcController.index().url)(authorised(AuthProviders(GovernmentGateway))({
      doThanks(request)
    }))
  }

  def thanksUnauthenticated = Action.async { implicit request =>
    doThanks(request)
  }

  private def doThanks(implicit request: Request[AnyRef]) = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.contact_hmrc_confirmation(ticketId))
    }
    Future.successful(result)
  }

  def contactHmrcPartialForm(submitUrl: String, csrfToken: String, service: Option[String], renderFormOnly: Option[Boolean]) = Action.async {
    implicit request =>
      Future.successful {
        Ok(views.html.partials.contact_hmrc_form(ContactHmrcForm.form.fill(ContactForm(request.headers.get("Referer").getOrElse("n/a"), csrfToken, service, None)), submitUrl, renderFormOnly))
      }
  }

  def submitContactHmrcPartialForm(resubmitUrl: String, renderFormOnly: Option[Boolean]) = Action.async {
    implicit request =>
      ContactHmrcForm.form.bindFromRequest()(request).fold(
        error => {
          Future.successful(BadRequest(views.html.partials.contact_hmrc_form(error, resubmitUrl, renderFormOnly)))
        },
        data => {
          (for {
            enrolments <- maybeAuthenticatedUserEnrolments()
            ticketId <- createDeskproTicket(data, enrolments)
          } yield {
            Ok(ticketId.ticket_id.toString)
          }).recover {
            case _ => InternalServerError(deskpro_error())
          }
        }
      )
  }

  def contactHmrcPartialFormConfirmation(ticketId: String) = UnauthorisedAction {
    implicit request =>
      Ok(views.html.partials.contact_hmrc_form_confirmation(ticketId))
  }
}

case class ContactForm(contactName: String,
                       contactEmail: String,
                       contactComments: String,
                       isJavascript: Boolean,
                       referer: String,
                       csrfToken: String,
                       service: Option[String] = Some("unknown"),
                       abFeatures: Option[String] = None)

object ContactForm {
  def apply(referer: String,
            csrfToken: String,
            service: Option[String],
            abFeatures: Option[String]): ContactForm =
    ContactForm("", "", "", isJavascript = false, referer, csrfToken, service, abFeatures)
}

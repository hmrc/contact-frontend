package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment, Logger}
import play.filters.csrf.CSRF
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util.{BackUrlValidator, DeskproEmailValidator}

import scala.concurrent.Future

@Singleton
class FeedbackController @Inject()(val hmrcDeskproConnector : HmrcDeskproConnector, val authConnector : AuthConnector, val accessibleUrlValidator : BackUrlValidator,
                                   val configuration : Configuration, val environment: Environment)(implicit val appConfig : AppConfig, override val messagesApi : MessagesApi)
extends FrontendController with DeskproSubmission with I18nSupport with AuthorisedFunctions with LoginRedirection  {

  val formId = "FeedbackForm"

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  def feedbackForm(service: Option[String] = None, backUrl: Option[String] = None) = Action.async { implicit request =>
    loginRedirection(routes.FeedbackController.feedbackForm(service, backUrl).url)(authorised(AuthProviders(GovernmentGateway)) {
      Future.successful(
        Ok(views.html.feedback(FeedbackForm.emptyForm(CSRF.getToken(request).map {
          _.value
        }.getOrElse(""), backUrl = backUrl), true, service, backUrl))
      )
    })
  }

  def unauthenticatedFeedbackForm(service: Option[String] = None, backUrl: Option[String] = None) = Action.async { implicit request =>
    Future.successful(
      Ok(views.html.feedback(FeedbackForm.emptyForm(CSRF.getToken(request).map { _.value }.getOrElse(""),
        backUrl = backUrl), false, service, backUrl))
    )
  }

  def submit = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.allEnrolments){
        allEnrolments =>
      doSubmit(Some(allEnrolments))
    }
  }

  def submitUnauthenticated = Action.async {
    implicit request => doSubmit(None)
  }

  def thanks(backUrl: Option[String] = None) = Action.async { implicit request =>

    val validatedBackUrl = backUrl.filter(accessibleUrlValidator.validate)

    loginRedirection(routes.FeedbackController.thanks(validatedBackUrl).url)(
    authorised(AuthProviders(GovernmentGateway)) { doThanks(true, request, validatedBackUrl)
    })
  }

  def unauthenticatedThanks(backUrl: Option[String] = None) = Action.async {
    implicit request => doThanks(false, request, backUrl)
  }

  private def doSubmit(enrolments: Option[Enrolments])(implicit request: Request[AnyContent]): Future[Result] =
    FeedbackFormBind.form.bindFromRequest()(request).fold(
      error => Future.successful(BadRequest(feedbackView(enrolments.isDefined, error))),
      data => {
        val ticketIdF = createDeskproFeedback(data, enrolments)
        ticketIdF map { ticketId =>
          Redirect(enrolments.map(_ => routes.FeedbackController.thanks(data.backUrl))
            .getOrElse(routes.FeedbackController.unauthenticatedThanks(data.backUrl))).
              withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
        }
      }
    )

  private def feedbackView(loggedIn : Boolean, form: Form[FeedbackForm])(implicit request: Request[AnyRef]) = {
    views.html.feedback(form, loggedIn, form("service").value, form("backUrl").value)
  }

  private def doThanks(implicit loggedIn : Boolean, request: Request[AnyRef], backUrl: Option[String] = None): Future[Result] = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.feedback_confirmation(ticketId, loggedIn, backUrl))
    }
    Future.successful(result)
  }

}

case class FeedbackForm(experienceRating: String, name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String, csrfToken: String, service: Option[String] = Some("unknown"), backUrl: Option[String])

object FeedbackForm {
  def apply(referer: String, csrfToken: String, backUrl : Option[String]): FeedbackForm =
    FeedbackForm("", "", "", "", javascriptEnabled = false, referer, csrfToken, backUrl = backUrl)

  def apply(
    experienceRating: Option[String], name: String, email: String, comments: String,
    javascriptEnabled: Boolean, referrer: String, csrfToken: String, service: Option[String],
    backUrl: Option[String]
  ): FeedbackForm =
    FeedbackForm(experienceRating.getOrElse(""), name, email, comments, javascriptEnabled, referrer, csrfToken, service,
      backUrl)

  def emptyForm(csrfToken: String, referer: Option[String] = None, backUrl : Option[String])(implicit request: Request[AnyRef]) =
    FeedbackFormBind.form.fill(FeedbackForm(referer.getOrElse(request.headers.get("Referer").getOrElse("n/a")), csrfToken,
      backUrl))
}

object FeedbackFormConfig {
  val validExperiences = (5 to 1 by -1) map (_.toString)
}

object FeedbackFormBind {

  import controllers.FeedbackFormConfig._

  private val emailValidator = new DeskproEmailValidator()
  private val validateEmail: (String) => Boolean = emailValidator.validate

  val form = Form[FeedbackForm](mapping(
    "feedback-rating" -> optional(text)
      .verifying("error.common.feedback.rating_mandatory", rating => rating.isDefined && !rating.get.trim.isEmpty)
      .verifying("error.common.feedback.rating_valid", rating => rating.map(validExperiences.contains(_)).getOrElse(true)),

    "feedback-name" -> text
      .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
      .verifying("error.common.feedback.name_too_long", name => name.size <= 70),

    "feedback-email" -> text
      .verifying("error.email", validateEmail)
      .verifying("deskpro.email_too_long", email => email.size <= 255),

    "feedback-comments" -> text
      .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
      .verifying("error.common.comments_too_long", comment => {
        val result = comment.size <= 2000
        Logger.error(s"Comment too long? ${result}")
        result
      }),

    "isJavascript" -> boolean,
    "referer" -> text,
    "csrfToken" -> text,
    "service" -> optional(text),
    "backUrl" -> optional(text)
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
      import feedbackForm._
      Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer, csrfToken, service, backUrl ))
    }))
}

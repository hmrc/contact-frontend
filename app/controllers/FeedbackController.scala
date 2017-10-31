package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request, Result}
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.validators.Validators

import scala.concurrent.Future

@Singleton
class FeedbackController @Inject() (val hmrcDeskproConnector : HmrcDeskproConnector, ggProvider : GovernmentGatewayAuthProvider)(implicit val authConnector : AuthConnector, appConfig : AppConfig, override val messagesApi : MessagesApi)
extends FrontendController
  with Actions with DeskproSubmission with I18nSupport {

  val formId = "FeedbackForm"

  val ggAuthProvider = ggProvider.forUrl(routes.FeedbackController.feedbackForm(None).url)

  def feedbackForm(service: Option[String] = None) = {
    WithNewSessionTimeout(
      AuthenticatedBy(ggAuthProvider, pageVisibility = GGConfidence).async { implicit user => implicit request =>
        Future.successful(
          Ok(views.html.feedback(FeedbackForm.emptyForm(CSRF.getToken(request).map { _.value }.getOrElse("")), Some(user), service))
        )
      }
    )
  }

  def unauthenticatedFeedbackForm(service: Option[String] = None) = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.feedback(FeedbackForm.emptyForm(CSRF.getToken(request).map { _.value }.getOrElse("")), None, service))
    )
  }

  def submit = WithNewSessionTimeout {
    AuthenticatedBy(ggAuthProvider, pageVisibility = GGConfidence).async { implicit user => implicit request =>
      doSubmit(Some(user))
    }
  }

  def submitUnauthenticated = UnauthorisedAction.async {
    implicit request => doSubmit(None)
  }

  val ggAuthProviderThanks = ggProvider.forUrl(routes.FeedbackController.thanks().url)

  def thanks = WithNewSessionTimeout {
    AuthenticatedBy(ggAuthProviderThanks, pageVisibility = GGConfidence).async { implicit user => implicit request => doThanks(Some(user), request)
    }
  }

  def unauthenticatedThanks = UnauthorisedAction.async {
    implicit request => doThanks(None, request)
  }

  private def doSubmit(user: Option[AuthContext])(implicit request: Request[AnyContent]): Future[Result] =
    FeedbackFormBind.form.bindFromRequest()(request).fold(
      error => Future.successful(BadRequest(feedbackView(user, error))),
      data => {
        val ticketIdF = createDeskproFeedback(data, user.map(_.principal.accounts))
        ticketIdF map { ticketId =>
          Redirect(user.map(_ => routes.FeedbackController.thanks()).getOrElse(routes.FeedbackController.unauthenticatedThanks())).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
        }
      }
    )

  private def feedbackView(user: Option[AuthContext], form: Form[FeedbackForm])(implicit request: Request[AnyRef]) = {
    views.html.feedback(form, user)
  }

  private def doThanks(implicit user: Option[AuthContext], request: Request[AnyRef]): Future[Result] = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.feedback_confirmation(ticketId, user))
    }
    Future.successful(result)
  }

}

case class FeedbackForm(experienceRating: String, name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String, csrfToken: String, service: Option[String] = Some("unknown"))

object FeedbackForm {
  def apply(referer: String, csrfToken: String): FeedbackForm =
    FeedbackForm("", "", "", "", javascriptEnabled = false, referer, csrfToken)

  def apply(
    experienceRating: Option[String], name: String, email: String, comments: String,
    javascriptEnabled: Boolean, referrer: String, csrfToken: String, service: Option[String]
  ): FeedbackForm =
    FeedbackForm(experienceRating.getOrElse(""), name, email, comments, javascriptEnabled, referrer, csrfToken, service)

  def emptyForm(csrfToken: String, referer: Option[String] = None)(implicit request: Request[AnyRef]) =
    FeedbackFormBind.form.fill(FeedbackForm(referer.getOrElse(request.headers.get("Referer").getOrElse("n/a")), csrfToken))
}

object FeedbackFormConfig {
  val validExperiences = (5 to 1 by -1) map (_.toString)
}

object FeedbackFormBind {

  import controllers.FeedbackFormConfig._

  val form = Form[FeedbackForm](mapping(
    "feedback-rating" -> optional(text)
      .verifying("error.common.feedback.rating_mandatory", rating => rating.isDefined && !rating.get.trim.isEmpty)
      .verifying("error.common.feedback.rating_valid", rating => rating.map(validExperiences.contains(_)).getOrElse(true)),

    "feedback-name" -> text
      .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
      .verifying("error.common.feedback.name_too_long", name => name.size <= 70),

    "feedback-email" -> Validators.emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),

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
    "service" -> optional(text)
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
      import feedbackForm._
      Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer, csrfToken, service))
    }))
}

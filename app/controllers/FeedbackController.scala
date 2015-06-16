package controllers

import config.FrontendAuthConnector
import connectors.deskpro.HmrcDeskproConnector
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Request, Result}
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.frontend.auth.{Actions, User}
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.validators.Validators

import scala.concurrent.Future

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
      .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
    "isJavascript" -> boolean,
    "referer" -> text,
    "csrfToken" -> text
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
    import feedbackForm._
    Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer, csrfToken))
  }))
}

trait FeedbackController
  extends FrontendController
  with Actions with DeskproSubmission {

  val formId = "FeedbackForm"

  val ggAuthProvider = new GovernmentGatewayAuthProvider(routes.FeedbackController.feedbackForm().url)

  def feedbackForm = WithNewSessionTimeout(AuthenticatedBy(ggAuthProvider).async {
    implicit user => implicit request =>
    Future.successful(
      Ok(views.html.feedback(FeedbackForm.emptyForm(CSRF.getToken(request).map{ _.value }.getOrElse("")), Some(user)))
    )
  })

  def unauthenticatedFeedbackForm = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.feedback(FeedbackForm.emptyForm(CSRF.getToken(request).map{ _.value }.getOrElse("")), None))
    )
  }

  def submit = WithNewSessionTimeout {
    AuthenticatedBy(ggAuthProvider).async {
      implicit user => implicit request =>
      doSubmit(Some(user))
    }
  }

  def submitUnauthenticated = UnauthorisedAction.async {
    implicit request => doSubmit(None)
  }

  val ggAuthProviderThanks = new GovernmentGatewayAuthProvider(routes.FeedbackController.thanks().url)

  def thanks = WithNewSessionTimeout {
    AuthenticatedBy(ggAuthProviderThanks).async {
      implicit user => implicit request => doThanks(Some(user), request)
    }
  }

  def unauthenticatedThanks = UnauthorisedAction.async {
    implicit request => doThanks(None, request)
  }

  private def doSubmit(user: Option[User])(implicit request: Request[AnyContent]): Future[Result] =
    FeedbackFormBind.form.bindFromRequest()(request).fold(
      error => Future.successful(BadRequest(feedbackView(user, error))),
      data => {
        val ticketIdF = createDeskproFeedback(data, user.map(_.userAuthority.accounts))
        ticketIdF map { ticketId =>
          Redirect(user.map(_ => routes.FeedbackController.thanks()).getOrElse(routes.FeedbackController.unauthenticatedThanks())).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
        }
      }
    )

  private def feedbackView(user: Option[User], form: Form[FeedbackForm])(implicit request: Request[AnyRef]) = {
    views.html.feedback(form, user)
  }

  private def doThanks(implicit user: Option[User], request: Request[AnyRef]): Future[Result] = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.feedback_confirmation(ticketId, user))
    }
    Future.successful(result)
  }

}

object FeedbackController extends FeedbackController {
  override val authConnector = FrontendAuthConnector
  override val hmrcDeskproConnector = HmrcDeskproConnector
}

case class FeedbackForm(experienceRating: String, name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String, csrfToken: String)

object FeedbackForm {
  def apply(referer: String, csrfToken: String): FeedbackForm = FeedbackForm("", "", "", "", javascriptEnabled = false, referer, csrfToken)

  def apply(experienceRating: Option[String], name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String, csrfToken: String): FeedbackForm =
    FeedbackForm(experienceRating.getOrElse(""), name, email, comments, javascriptEnabled, referrer, csrfToken)

  def emptyForm(csrfToken: String)(implicit request: Request[AnyRef]) = FeedbackFormBind.form.fill(FeedbackForm(request.headers.get("Referer").getOrElse("n/a"), csrfToken))
}

object FeedbackFormConfig {
  val validExperiences = (5 to 1 by -1) map (_.toString)
  val feedbackRatings = validExperiences zip Seq("Very good", "Good", "Neutral", "Bad", "Very bad")
}

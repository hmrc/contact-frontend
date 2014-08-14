package controllers

import controllers.common.{GovernmentGateway, AuthenticationProvider, AnyAuthenticationProvider}
import controllers.common.actions.Actions
import controllers.common.validators.Validators._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Controller
import uk.gov.hmrc.common.microservice.auth.AuthConnector

import scala.concurrent.Future


class FeedbackController extends Controller with Actions {
  override protected implicit def authConnector = new AuthConnector()

  import FeedbackFormConfig._

  val form = Form[FeedbackForm](mapping(
    "feedback-rating" -> optional(text)
      .verifying("error.common.feedback.rating_mandatory", rating => rating.isDefined && !rating.get.trim.isEmpty)
      .verifying("error.common.feedback.rating_valid", rating => rating.map(validExperiences.contains(_)).getOrElse(true)),
    "feedback-name" -> text
      .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
      .verifying("error.common.feedback.name_too_long", name => name.size <= 70),
    "feedback-email" -> emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),
    "feedback-comments" -> text
      .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
      .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
    "isJavascript" -> boolean,
    "referer" -> text
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
    import feedbackForm._
    Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer))
  }))

  def feedbackForm = WithNewSessionTimeout(AuthenticatedBy(AuthProvider).async { implicit user => implicit request =>
    Future.successful(
      Ok(views.html.feedback(form, Some(user)))
    )
  })

}

object AuthProvider extends GovernmentGateway {
  override def login: String = "http://localhost:11111/stub/sign-in"
}

case class FeedbackForm(experienceRating: String, name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String)

object FeedbackForm {
  def apply(referer: String): FeedbackForm = FeedbackForm("", "", "", "", javascriptEnabled = false, referer)

  def apply(experienceRating: Option[String], name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String): FeedbackForm =
    FeedbackForm(experienceRating.getOrElse(""), name, email, comments, javascriptEnabled, referrer)
}

object FeedbackFormConfig {
  val validExperiences = (5 to 1 by -1) map (_.toString)
  val feedbackRatings = validExperiences zip Seq("Very good", "Good", "Neutral", "Bad", "Very bad")
}

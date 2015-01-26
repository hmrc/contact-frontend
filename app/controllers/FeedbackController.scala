package controllers

import controllers.common.{FrontEndRedirect, GovernmentGateway, AnyAuthenticationProvider}
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Controller, Request, Result}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.auth.AuthConnector
import uk.gov.hmrc.play.microservice.domain.User
import uk.gov.hmrc.play.validators.Validators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeedbackController
  extends Controller
  with Actions {

  override implicit def authConnector: AuthConnector = Connectors.authConnector

  lazy val hmrcDeskproConnector = Connectors.hmrcDeskproConnector

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)

  import controllers.FeedbackFormConfig._

  val formId = "FeedbackForm"

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
    "referer" -> text
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
    import feedbackForm._
    Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer))
  }))

  def feedbackForm = WithNewSessionTimeout(AuthenticatedBy(new GovernmentGatewayAuthProvider(routes.FeedbackController.feedbackForm().url)).async {
    implicit user => implicit request =>
    Future.successful(
      Ok(views.html.feedback(emptyForm, Some(user)))
    )
  })

  def unauthenticatedFeedbackForm = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.feedback(emptyForm, None))
    )
  }

  def submit = WithNewSessionTimeout {
    AuthenticatedBy(new GovernmentGatewayAuthProvider(routes.FeedbackController.feedbackForm().url)).async {
      implicit user => implicit request =>
      doSubmit(Some(user))
    }
  }

  def submitUnauthenticated = UnauthorisedAction.async {
    implicit request => doSubmit(None)
  }

  def thanks = WithNewSessionTimeout {
    AuthenticatedBy(new GovernmentGatewayAuthProvider(routes.FeedbackController.thanks().url)).async {
      implicit user => implicit request => doThanks(Some(user), request)
    }
  }

  def unauthenticatedThanks = UnauthorisedAction.async {
    implicit request => doThanks(None, request)
  }

  private def doSubmit(user: Option[User])(implicit request: Request[AnyRef]): Future[Result] =
    form.bindFromRequest()(request).fold(
      error => Future.successful(BadRequest(feedbackView(user, error))),
      data => {
        import data._

        val ticketIdF = hmrcDeskproConnector.createFeedback(name, email, experienceRating, "Beta feedback submission", comments, referrer, javascriptEnabled, request, user)

        ticketIdF map {
          case Some(ticketId) => Redirect(user.map(_ => routes.FeedbackController.thanks()).getOrElse(routes.FeedbackController.unauthenticatedThanks())).withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
          case None => InternalServerError("Deskpro failure")
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

  private def emptyForm(implicit request: Request[AnyRef]) = form.fill(FeedbackForm(request.headers.get("Referer").getOrElse("n/a")))

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

package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import javax.inject.{Inject, Singleton}
import model.FeedbackForm
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.{Configuration, Environment}
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, UnauthorisedAction}
import util.{BackUrlValidator, DeskproEmailValidator}

import scala.concurrent.Future

@Singleton
class FeedbackController @Inject()(
  val hmrcDeskproConnector: HmrcDeskproConnector,
  val authConnector: AuthConnector,
  val accessibleUrlValidator: BackUrlValidator,
  val configuration: Configuration,
  val environment: Environment)(implicit val appConfig: AppConfig, override val messagesApi: MessagesApi)
    extends FrontendController
    with DeskproSubmission
    with I18nSupport
    with AuthorisedFunctions
    with LoginRedirection
    with ContactFrontendActions {

  val formId = "FeedbackForm"

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  def feedbackForm(service: Option[String] = None, backUrl: Option[String] = None, canOmitComments: Boolean) =
    Action.async { implicit request =>
      loginRedirection(routes.FeedbackController.feedbackForm(service, backUrl).url)(
        authorised(AuthProviders(GovernmentGateway)) {
          Future.successful(
            Ok(
              views.html.feedback(
                FeedbackFormBind.emptyForm(
                  CSRF
                    .getToken(request)
                    .map {
                      _.value
                    }
                    .getOrElse(""),
                  backUrl         = backUrl,
                  canOmitComments = canOmitComments),
                loggedIn = true,
                service,
                backUrl,
                canOmitComments = canOmitComments
              ))
          )
        })
    }

  def unauthenticatedFeedbackForm(
    service: Option[String] = None,
    backUrl: Option[String] = None,
    canOmitComments: Boolean) = Action.async { implicit request =>
    Future.successful(
      Ok(
        views.html.feedback(
          FeedbackFormBind.emptyForm(
            CSRF.getToken(request).map { _.value }.getOrElse(""),
            backUrl         = backUrl,
            canOmitComments = canOmitComments),
          loggedIn = false,
          service,
          backUrl,
          canOmitComments = canOmitComments
        ))
    )
  }

  def submit = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.allEnrolments) { allEnrolments =>
      doSubmit(Some(allEnrolments))
    }
  }

  def submitUnauthenticated = Action.async { implicit request =>
    doSubmit(None)
  }

  def thanks(backUrl: Option[String] = None) = Action.async { implicit request =>
    val validatedBackUrl = backUrl.filter(accessibleUrlValidator.validate)

    loginRedirection(routes.FeedbackController.thanks(validatedBackUrl).url)(
      authorised(AuthProviders(GovernmentGateway)) { doThanks(true, request, validatedBackUrl) })
  }

  def unauthenticatedThanks(backUrl: Option[String] = None) = Action.async { implicit request =>
    val validatedBackUrl = backUrl.filter(accessibleUrlValidator.validate)
    doThanks(false, request, validatedBackUrl)
  }

  private def doSubmit(enrolments: Option[Enrolments])(implicit request: Request[AnyContent]): Future[Result] =
    FeedbackFormBind.form
      .bindFromRequest()(request)
      .fold(
        error => Future.successful(BadRequest(feedbackView(enrolments.isDefined, error))),
        data => {
          val ticketIdF = createDeskproFeedback(data, enrolments)
          ticketIdF map { ticketId =>
            Redirect(
              enrolments
                .map(_ => routes.FeedbackController.thanks(data.backUrl))
                .getOrElse(routes.FeedbackController.unauthenticatedThanks(data.backUrl)))
              .withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
          }
        }
      )

  private def feedbackView(loggedIn: Boolean, form: Form[FeedbackForm])(implicit request: Request[AnyRef]) =
    views.html.feedback(
      form,
      loggedIn,
      form("service").value,
      form("backUrl").value,
      form("canOmitComments").value.exists(_ == "true"))

  private def doThanks(
    implicit loggedIn: Boolean,
    request: Request[AnyRef],
    backUrl: Option[String] = None): Future[Result] = {
    val result = request.session.get("ticketId").fold(BadRequest("Invalid data")) { ticketId =>
      Ok(views.html.feedback_confirmation(ticketId, loggedIn, backUrl))
    }
    Future.successful(result)
  }

  def feedbackPartialForm(
    submitUrl: String,
    csrfToken: String,
    service: Option[String],
    referer: Option[String],
    canOmitComments: Boolean) = Action.async { implicit request =>
    Future.successful {
      Ok(
        views.html.partials.feedback_form(
          FeedbackFormBind.emptyForm(csrfToken, referer, None, canOmitComments = canOmitComments),
          submitUrl,
          service,
          canOmitComments = canOmitComments))
    }
  }

  def submitFeedbackPartialForm(resubmitUrl: String) = Action.async { implicit request =>
    val form = FeedbackFormBind.form.bindFromRequest()(request)
    form.fold(
      error => {
        Future.successful(
          BadRequest(views.html.partials
            .feedback_form(error, resubmitUrl, canOmitComments = form("canOmitComments").value.exists(_ == "true"))))
      },
      data => {
        (for {
          enrolments <- maybeAuthenticatedUserEnrolments()
          ticketId   <- createDeskproFeedback(data, enrolments)
        } yield {
          Ok(ticketId.ticket_id.toString)
        }).recover {
          case _ => InternalServerError
        }
      }
    )
  }

  def feedbackPartialFormConfirmation(ticketId: String) = UnauthorisedAction { implicit request =>
    Ok(views.html.partials.feedback_form_confirmation(ticketId, None))
  }

}

object FeedbackFormBind {

  import model.FeedbackFormConfig._

  private val emailValidator                     = new DeskproEmailValidator()
  private val validateEmail: (String) => Boolean = emailValidator.validate

  def emptyForm(csrfToken: String, referer: Option[String] = None, backUrl: Option[String], canOmitComments: Boolean)(
    implicit request: Request[AnyRef]) =
    FeedbackFormBind.form.fill(
      FeedbackForm(
        referer.getOrElse(request.headers.get("Referer").getOrElse("n/a")),
        csrfToken,
        backUrl,
        canOmitComments))

  def form =
    Form[FeedbackForm](
      mapping(
        "feedback-rating" -> optional(text)
          .verifying("error.common.feedback.rating_mandatory", rating => rating.isDefined && !rating.get.trim.isEmpty)
          .verifying(
            "error.common.feedback.rating_valid",
            rating => rating.map(validExperiences.contains(_)).getOrElse(true)),
        "feedback-name" -> text
          .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
          .verifying("error.common.feedback.name_too_long", name => name.size <= 70),
        "feedback-email" -> text
          .verifying("error.email", validateEmail)
          .verifying("deskpro.email_too_long", email => email.size <= 255),
        "feedback-comments" -> FieldMapping[String]()(new Formatter[String] {

          override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
            val commentsCanBeOmitted = data.get("canOmitComments").contains("true")
            data.get(key) match {
              case Some(value) if !value.trim.isEmpty || commentsCanBeOmitted => Right(value.trim)
              case Some(_)                                                    => Left(Seq(FormError(key, "error.common.comments_mandatory", Nil)))
              case None                                                       => Left(Seq(FormError(key, "error.required", Nil)))
            }
          }
          override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)

        }).verifying("error.common.comments_too_long", comment => {
          val result = comment.size <= 2000
          result
        }),
        "isJavascript"    -> boolean,
        "referer"         -> text,
        "csrfToken"       -> text,
        "service"         -> optional(text),
        "abFeatures"      -> optional(text),
        "backUrl"         -> optional(text),
        "canOmitComments" -> boolean
      )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
        import feedbackForm._
        Some(
          (
            Some(experienceRating),
            name,
            email,
            comments,
            javascriptEnabled,
            referrer,
            csrfToken,
            service,
            abFeatures,
            backUrl,
            canOmitComments))
      }))
}

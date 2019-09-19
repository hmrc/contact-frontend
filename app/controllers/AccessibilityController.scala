package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import javax.inject.Inject
import model.AccessibilityForm
import play.api.Mode.Mode
import play.api.data.Form
import play.api.data.Forms._
import play.api.{Configuration, Environment}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util.{BackUrlValidator, DeskproEmailValidator}

import scala.concurrent.{ExecutionContext, Future}

object AccessibilityFormBind {
  private val emailValidator                     = DeskproEmailValidator()
  private val validateEmail: String => Boolean = emailValidator.validate

  def emptyForm(csrfToken: String,
                referer: Option[String] = None,
                service: Option[String] = None,
                userAction :Option[String] = None)
               (implicit request: Request[_]): Form[AccessibilityForm] = {
    AccessibilityFormBind.form.fill(
      AccessibilityForm(
        problemDescription = "",
        name = "",
        email = "",
        isJavascript = false,
        referrer = referer.getOrElse("n/a"),
        csrfToken = csrfToken,
        service = service,
        userAction = userAction
      ))
  }

  def form(implicit request: Request[_]): Form[AccessibilityForm] = Form[AccessibilityForm](
    mapping(
      "problemDescription" -> text
        .verifying("error.common.accessibility.problem.toolong", msg => msg.length <= 2000)
        .verifying("error.common.accessibility.problem.required", msg => !msg.trim.isEmpty),
      "name"         -> text
        .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
        .verifying("error.common.feedback.name_too_long", name => name.length <= 70),
      "email"        -> text
        .verifying("error.email", validateEmail)
        .verifying("deskpro.email_too_long", email => email.length <= 255),
      "isJavascript" -> boolean,
      "referer"      -> text,
      "csrfToken"    -> text,
      "service"      -> optional(text),
      "userAction"   -> optional(text)

    )(AccessibilityForm.apply)(AccessibilityForm.unapply))
}

class AccessibilityController @Inject()(val hmrcDeskproConnector: HmrcDeskproConnector,
                                        val authConnector: AuthConnector,
                                        val accessibleUrlValidator: BackUrlValidator,
                                        val configuration: Configuration,
                                        val environment: Environment)(implicit val appConfig: AppConfig, override val messagesApi: MessagesApi)
    extends FrontendController
    with DeskproSubmission
    with I18nSupport
    with AuthorisedFunctions
    with LoginRedirection
    with ContactFrontendActions
{

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  /** Authenticated routes */

  def accessibilityForm(service: Option[String], userAction: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    if( request.session.get(SessionKeys.authToken).isEmpty) {
      Future.successful {
        Redirect(routes.AccessibilityController.unauthenticatedAccessibilityForm(service, userAction))
      }
    } else {
      loginRedirection(routes.AccessibilityController.accessibilityForm(service, userAction).url)(
        authorised(AuthProviders(GovernmentGateway)) {

          Future.successful {
            val referer   = request.headers.get("Referer")
            val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
            val form      = AccessibilityFormBind.emptyForm(csrfToken, referer, service, userAction)
            Ok(views.html.accessibility(form, routes.AccessibilityController.submitAccessibilityForm().url, loggedIn = true))
          }

        })
    }
  }

  def submitAccessibilityForm(): Action[AnyContent] = Action.async { implicit request =>
    loginRedirection(routes.AccessibilityController.submitAccessibilityForm().url)(
      authorised(AuthProviders(GovernmentGateway)) {

        AccessibilityFormBind.form
          .bindFromRequest()(request)
          .fold(
            (error: Form[AccessibilityForm]) =>
              Future.successful(BadRequest(views.html.accessibility(error, routes.AccessibilityController.submitAccessibilityForm().url, loggedIn = true))),
            data => {
              for {
                maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
                _                   <- createAccessibilityTicket(data, maybeUserEnrolments)
                thanks               = routes.AccessibilityController.thanks()
              } yield Redirect(thanks)
            }
          )

      })
  }

  def thanks(): Action[AnyContent] = Action.async { implicit request =>
    loginRedirection(routes.AccessibilityController.thanks().url)(
      authorised(AuthProviders(GovernmentGateway)) {
        Future.successful(Ok(views.html.accessibility_confirmation("", loggedIn = true)))
      })
  }


  /** Unauthenticated routes */

  def unauthenticatedAccessibilityForm(service: Option[String], userAction: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful {
      val referer   = request.headers.get("Referer")
      val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
      val form      = AccessibilityFormBind.emptyForm(csrfToken, referer, service, userAction)
      Ok(views.html.accessibility(form, routes.AccessibilityController.submitUnauthenticatedAccessibilityForm().url, loggedIn = false))
    }
  }

  def submitUnauthenticatedAccessibilityForm(): Action[AnyContent] = Action.async { implicit request =>
    AccessibilityFormBind.form
      .bindFromRequest()(request)
      .fold(
        (error: Form[AccessibilityForm]) =>
          Future.successful(BadRequest(views.html.accessibility(error, routes.AccessibilityController.submitUnauthenticatedAccessibilityForm().url, loggedIn = false))),
        data => {
          for {
            _      <- createAccessibilityTicket(data, None)
            thanks  = routes.AccessibilityController.unauthenticatedThanks()
          } yield Redirect(thanks)
        }
      )
  }

  def unauthenticatedThanks(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.html.accessibility_confirmation("")))
  }

}

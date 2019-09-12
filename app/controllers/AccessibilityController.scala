package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import javax.inject.Inject
import model.AccessibilityForm
import play.api.data.Form
import play.api.data.Forms._
import play.api.{Configuration, Environment}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Request}
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import util.{BackUrlValidator, DeskproEmailValidator}

import scala.concurrent.Future

object AccessibilityFormBind {
  private val emailValidator                     = new DeskproEmailValidator()
  private val validateEmail: (String) => Boolean = emailValidator.validate

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

  def form(implicit request: Request[_]) = Form[AccessibilityForm](
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
{

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  def accessibilityForm(service: Option[String], userAction: Option[String]) = Action.async { implicit request =>
    Future.successful{
      val referer   = request.headers.get("Referer")
      val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
      val form      = AccessibilityFormBind.emptyForm(csrfToken, referer, service, userAction)
      Ok(views.html.accessibility(form, routes.AccessibilityController.submitAccessibilityForm().url))
    }
  }

  def submitAccessibilityForm() = Action.async { implicit request =>
    AccessibilityFormBind.form
      .bindFromRequest()(request)
        .fold(
          (error: Form[AccessibilityForm]) =>
            Future.successful(BadRequest(views.html.accessibility(error, routes.AccessibilityController.submitAccessibilityForm().url))),
          data => {
            for {
              _ <- createAccessibilityTicket(data, None)
            } yield Redirect(routes.AccessibilityController.thanks())
          }
        )
  }

  def thanks() = Action.async { implicit request =>
    Future.successful(Ok(views.html.accessibility_confirmation("")))
  }
}

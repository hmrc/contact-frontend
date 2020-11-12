/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import connectors.deskpro.HmrcDeskproConnector
import javax.inject.Inject
import model.AccessibilityForm
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request}
import play.filters.csrf.CSRF
import play.twirl.api.Html
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.DeskproEmailValidator
import views.html.{AccessibilityProblemConfirmationPage, AccessibilityProblemPage, accessibility, accessibility_confirmation}

import scala.concurrent.{ExecutionContext, Future}

object AccessibilityFormBind {
  private val emailValidator                   = DeskproEmailValidator()
  private val validateEmail: String => Boolean = emailValidator.validate

  def emptyForm(
    csrfToken: String,
    referrer: Option[String] = None,
    service: Option[String] = None,
    userAction: Option[String] = None
  ): Form[AccessibilityForm] =
    AccessibilityFormBind.form.fill(
      AccessibilityForm(
        problemDescription = "",
        name = "",
        email = "",
        isJavascript = false,
        referrer = referrer.getOrElse("n/a"),
        csrfToken = csrfToken,
        service = service,
        userAction = userAction
      )
    )

  def form: Form[AccessibilityForm] = Form[AccessibilityForm](
    mapping(
      "problemDescription" -> text
        .verifying("accessibility.problem.error.required", msg => !msg.trim.isEmpty)
        .verifying("accessibility.problem.error.length", msg => msg.length <= 2000),
      "name"               -> text
        .verifying("accessibility.name.error.required", name => !name.trim.isEmpty)
        .verifying("accessibility.name.error.length", name => name.length <= 70),
      "email"              -> text
        .verifying("accessibility.email.error.required", name => !name.trim.isEmpty)
        .verifying("accessibility.email.error.invalid", validateEmail)
        .verifying("accessibility.email.error.length", email => email.length <= 255),
      "isJavascript"       -> boolean,
      "referrer"           -> text,
      "csrfToken"          -> text,
      "service"            -> optional(text),
      "userAction"         -> optional(text)
    )(AccessibilityForm.apply)(AccessibilityForm.unapply)
  )
}

class AccessibilityController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  val authConnector: AuthConnector,
  val configuration: Configuration,
  mcc: MessagesControllerComponents,
  assetsFrontendAccessibilityPage: accessibility,
  assetsFrontendAccessibilityConfirmationPage: accessibility_confirmation,
  playFrontendAccessibilityProblemPage: AccessibilityProblemPage,
  playFrontendAccessibilityProblemConfirmationPage: AccessibilityProblemConfirmationPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport
    with AuthorisedFunctions
    with LoginRedirection
    with ContactFrontendActions {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  /** Authenticated routes */

  def accessibilityForm(service: Option[String], userAction: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      if (request.session.get(SessionKeys.authToken).isEmpty) {
        Future.successful {
          Redirect(routes.AccessibilityController.unauthenticatedAccessibilityForm(service, userAction, None))
        }
      } else {
        loginRedirection(routes.AccessibilityController.accessibilityForm(service, userAction).url)(
          authorised(AuthProviders(GovernmentGateway)) {

            Future.successful {
              val referrer  = request.headers.get(REFERER)
              val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
              val form      = AccessibilityFormBind.emptyForm(csrfToken, referrer, service, userAction)
              Ok(accessibilityPage(form, routes.AccessibilityController.submitAccessibilityForm(), loggedIn = true))
            }

          }
        )
      }
  }

  def submitAccessibilityForm(): Action[AnyContent] = Action.async { implicit request =>
    loginRedirection(routes.AccessibilityController.submitAccessibilityForm().url)(
      authorised(AuthProviders(GovernmentGateway)) {

        AccessibilityFormBind.form
          .bindFromRequest()(request)
          .fold(
            (error: Form[AccessibilityForm]) =>
              Future.successful(
                BadRequest(
                  accessibilityPage(
                    error,
                    routes.AccessibilityController.submitAccessibilityForm(),
                    loggedIn = true
                  )
                )
              ),
            data =>
              for {
                maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
                _                   <- createAccessibilityTicket(data, maybeUserEnrolments)
                thanks               = routes.AccessibilityController.thanks()
              } yield Redirect(thanks)
          )

      }
    )
  }

  def thanks(): Action[AnyContent] = Action.async { implicit request =>
    loginRedirection(routes.AccessibilityController.thanks().url)(authorised(AuthProviders(GovernmentGateway)) {
      Future.successful(Ok(accessibilityConfirmationPage(loggedIn = true)))
    })
  }

  /** Unauthenticated routes */

  def unauthenticatedAccessibilityForm(
    service: Option[String],
    userAction: Option[String],
    referrerUrl: Option[String]
  ): Action[AnyContent] = Action.async { implicit request =>
    Future.successful {
      val referrer  = referrerUrl orElse request.headers.get(REFERER)
      val csrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
      val form      = AccessibilityFormBind.emptyForm(csrfToken, referrer, service, userAction)
      Ok(
        accessibilityPage(
          form,
          routes.AccessibilityController.submitUnauthenticatedAccessibilityForm(),
          loggedIn = false
        )
      )
    }
  }

  def submitUnauthenticatedAccessibilityForm(): Action[AnyContent] = Action.async { implicit request =>
    AccessibilityFormBind.form
      .bindFromRequest()(request)
      .fold(
        (error: Form[AccessibilityForm]) =>
          Future.successful(
            BadRequest(
              accessibilityPage(
                error,
                routes.AccessibilityController.submitUnauthenticatedAccessibilityForm(),
                loggedIn = false
              )
            )
          ),
        data =>
          for {
            _     <- createAccessibilityTicket(data, None)
            thanks = routes.AccessibilityController.unauthenticatedThanks()
          } yield Redirect(thanks)
      )
  }

  def unauthenticatedThanks(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(accessibilityConfirmationPage(loggedIn = false)))
  }

  private def accessibilityPage(form: Form[AccessibilityForm], action: Call, loggedIn: Boolean)(implicit
    request: Request[_],
    lang: Lang
  ): Html =
    if (appConfig.enablePlayFrontendAccessibilityForm) {
      playFrontendAccessibilityProblemPage(
        form,
        action
      )
    } else {
      assetsFrontendAccessibilityPage(form, action.url, loggedIn)
    }

  private def accessibilityConfirmationPage(loggedIn: Boolean)(implicit
    request: Request[_],
    lang: Lang
  ): Html =
    if (appConfig.enablePlayFrontendAccessibilityForm) {
      playFrontendAccessibilityProblemConfirmationPage()
    } else {
      assetsFrontendAccessibilityConfirmationPage("", loggedIn = false)
    }
}

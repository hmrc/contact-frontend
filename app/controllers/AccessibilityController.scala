/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import views.html.{AccessibilityProblemConfirmationPage, AccessibilityProblemPage, InternalErrorPage}

import scala.concurrent.{ExecutionContext, Future}

object AccessibilityFormBind {
  private val emailValidator = DeskproEmailValidator()

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
        .verifying("accessibility.problem.error.required", msg => msg.trim.nonEmpty)
        .verifying("accessibility.problem.error.length", msg => msg.length <= 2000),
      "name"               -> text
        .verifying("accessibility.name.error.required", name => name.trim.nonEmpty)
        .verifying("accessibility.name.error.length", name => name.length <= 70),
      "email"              -> text
        .verifying("accessibility.email.error.required", name => name.trim.nonEmpty)
        // the logic below ensures that two or more errors will not fire at the same time. This prevents
        // multiple error messages appearing in the error summary for the same field
        .verifying("accessibility.email.error.invalid", email => email.trim.isEmpty || emailValidator.validate(email))
        .verifying(
          "accessibility.email.error.length",
          email => !(email.trim.isEmpty || emailValidator.validate(email)) || email.length <= 255
        ),
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
  playFrontendAccessibilityProblemPage: AccessibilityProblemPage,
  playFrontendAccessibilityProblemConfirmationPage: AccessibilityProblemConfirmationPage,
  errorPage: InternalErrorPage
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
              val action    = routes.AccessibilityController.submitAccessibilityForm(service, userAction)
              Ok(accessibilityPage(form, action, loggedIn = true))
            }
          }
        )
      }
  }

  def submitAccessibilityForm(service: Option[String], userAction: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      val action = routes.AccessibilityController.submitAccessibilityForm(service, userAction)
      loginRedirection(action.url)(
        authorised(AuthProviders(GovernmentGateway)) {
          AccessibilityFormBind.form
            .bindFromRequest()
            .fold(
              (error: Form[AccessibilityForm]) =>
                Future.successful(
                  BadRequest(accessibilityPage(error, action, loggedIn = true))
                ),
              data =>
                {
                  for {
                    maybeUserEnrolments <- maybeAuthenticatedUserEnrolments
                    _                   <- createAccessibilityTicket(data, maybeUserEnrolments)
                    thanks               = routes.AccessibilityController.thanks()
                  } yield Redirect(thanks)
                }.recover { case _ =>
                  InternalServerError(errorPage())
                }
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
          routes.AccessibilityController.submitUnauthenticatedAccessibilityForm(service, userAction),
          loggedIn = false
        )
      )
    }
  }

  def submitUnauthenticatedAccessibilityForm(service: Option[String], userAction: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      AccessibilityFormBind.form
        .bindFromRequest()
        .fold(
          (error: Form[AccessibilityForm]) =>
            Future.successful(
              BadRequest(
                accessibilityPage(
                  error,
                  routes.AccessibilityController.submitUnauthenticatedAccessibilityForm(service, userAction),
                  loggedIn = false
                )
              )
            ),
          data =>
            {
              for {
                _     <- createAccessibilityTicket(data, None)
                thanks = routes.AccessibilityController.unauthenticatedThanks()
              } yield Redirect(thanks)
            }.recover { case _ =>
              InternalServerError(errorPage())
            }
        )
    }

  def unauthenticatedThanks(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(accessibilityConfirmationPage(loggedIn = false)))
  }

  private def accessibilityPage(form: Form[AccessibilityForm], action: Call, loggedIn: Boolean)(implicit
    request: Request[_]
  ): Html =
    playFrontendAccessibilityProblemPage(
      form,
      action
    )

  private def accessibilityConfirmationPage(loggedIn: Boolean)(implicit
    request: Request[_]
  ): Html =
    playFrontendAccessibilityProblemConfirmationPage()
}

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

import javax.inject.{Inject, Singleton}
import model.FeedbackForm
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import play.api.Configuration
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{BackUrlValidator, DeskproEmailValidator}
import views.html.partials.{feedback_form, feedback_form_confirmation}
import views.html.{FeedbackConfirmationPage, FeedbackPage, InternalErrorPage}
import play.api.http.HeaderNames._
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

@Singleton class FeedbackController @Inject() (
  val hmrcDeskproConnector: HmrcDeskproConnector,
  val authConnector: AuthConnector,
  val accessibleUrlValidator: BackUrlValidator,
  val configuration: Configuration,
  mcc: MessagesControllerComponents,
  playFrontendFeedbackConfirmationPage: FeedbackConfirmationPage,
  feedbackFormPartial: feedback_form,
  feedbackFormConfirmationPartial: feedback_form_confirmation,
  playFrontendFeedbackPage: FeedbackPage,
  errorPage: InternalErrorPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport
    with AuthorisedFunctions
    with LoginRedirection
    with ContactFrontendActions {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  val formId = "FeedbackForm"

  def feedbackForm(service: Option[String] = None, backUrl: Option[String] = None, canOmitComments: Boolean) =
    Action.async { implicit request =>
      loginRedirection(routes.FeedbackController.feedbackForm(service, backUrl).url)(
        authorised(AuthProviders(GovernmentGateway)) {
          Future.successful(
            Ok(
              feedbackPage(
                FeedbackFormBind.emptyForm(
                  CSRF
                    .getToken(request)
                    .map {
                      _.value
                    }
                    .getOrElse(""),
                  backUrl = backUrl,
                  canOmitComments = canOmitComments,
                  service = service
                ),
                loggedIn = true,
                service,
                backUrl,
                canOmitComments = canOmitComments
              )
            )
          )
        }
      )
    }

  def unauthenticatedFeedbackForm(
    service: Option[String] = None,
    backUrl: Option[String] = None,
    canOmitComments: Boolean
  ) = Action.async { implicit request =>
    Future.successful(
      Ok(
        feedbackPage(
          FeedbackFormBind.emptyForm(
            CSRF.getToken(request).map(_.value).getOrElse(""),
            backUrl = backUrl,
            canOmitComments = canOmitComments,
            service = service
          ),
          loggedIn = false,
          service,
          backUrl,
          canOmitComments = canOmitComments
        )
      )
    )
  }

  def submitDeprecated = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.allEnrolments) { allEnrolments =>
      doSubmit(Some(allEnrolments))
    }
  }

  def submitUnauthenticatedDeprecated = Action.async { implicit request =>
    doSubmit(None)
  }

  def submit(service: Option[String] = None, backUrl: Option[String] = None, canOmitComments: Boolean = false) =
    Action.async { implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.allEnrolments) { allEnrolments =>
        doSubmit(Some(allEnrolments))
      }
    }

  def submitUnauthenticated(
    service: Option[String] = None,
    backUrl: Option[String] = None,
    canOmitComments: Boolean = false
  ) = Action.async { implicit request =>
    doSubmit(None)
  }

  def thanks(backUrl: Option[String] = None) = Action.async { implicit request =>
    val validatedBackUrl = backUrl.filter(accessibleUrlValidator.validate)

    loginRedirection(routes.FeedbackController.thanks(validatedBackUrl).url)(
      authorised(AuthProviders(GovernmentGateway))(doThanks(true, request, validatedBackUrl))
    )
  }

  def unauthenticatedThanks(backUrl: Option[String] = None) = Action.async { implicit request =>
    val validatedBackUrl = backUrl.filter(accessibleUrlValidator.validate)
    doThanks(false, request, validatedBackUrl)
  }

  private def doSubmit(enrolments: Option[Enrolments])(implicit request: Request[AnyContent]): Future[Result] =
    FeedbackFormBind.form
      .bindFromRequest()
      .fold(
        error => Future.successful(BadRequest(feedbackView(enrolments.isDefined, error))),
        data =>
          {
            val ticketIdF = createDeskproFeedback(data, enrolments)
            ticketIdF map { ticketId =>
              Redirect(
                enrolments
                  .map(_ => routes.FeedbackController.thanks(data.backUrl))
                  .getOrElse(routes.FeedbackController.unauthenticatedThanks(data.backUrl))
              )
                .withSession(request.session + ("ticketId" -> ticketId.ticket_id.toString))
            }
          }.recover { case _ =>
            InternalServerError(errorPage())
          }
      )

  private def feedbackView(loggedIn: Boolean, form: Form[FeedbackForm])(implicit request: Request[AnyRef]) =
    feedbackPage(
      form,
      loggedIn,
      form("service").value,
      form("backUrl").value,
      form("canOmitComments").value.exists(_ == "true")
    )

  private def doThanks(implicit
    loggedIn: Boolean,
    request: Request[AnyRef],
    backUrl: Option[String]
  ): Future[Result] = {
    val maybeTicketId = request.session.get("ticketId")
    val result        =
      feedbackConfirmationPage(maybeTicketId, loggedIn, backUrl)
    Future.successful(result)
  }

  def feedbackPartialForm(
    submitUrl: String,
    csrfToken: String,
    service: Option[String],
    referer: Option[String],
    canOmitComments: Boolean,
    referrerUrl: Option[String]
  ) = Action.async { implicit request =>
    Future.successful {
      Ok(
        feedbackFormPartial(
          FeedbackFormBind.emptyForm(
            csrfToken,
            referrerUrl orElse referer,
            None,
            canOmitComments = canOmitComments,
            service = service
          ),
          submitUrl,
          service,
          canOmitComments = canOmitComments
        )
      )
    }
  }

  def submitFeedbackPartialForm(resubmitUrl: String) = Action.async { implicit request =>
    val form = FeedbackFormBind.form.bindFromRequest()
    form.fold(
      error =>
        Future.successful(
          BadRequest(
            feedbackFormPartial(error, resubmitUrl, canOmitComments = form("canOmitComments").value.exists(_ == "true"))
          )
        ),
      data =>
        (for {
          enrolments <- maybeAuthenticatedUserEnrolments()
          ticketId   <- createDeskproFeedback(data, enrolments)
        } yield Ok(ticketId.ticket_id.toString)).recover { case _ =>
          InternalServerError
        }
    )
  }

  def feedbackPartialFormConfirmation(ticketId: String) = Action { implicit request =>
    Ok(feedbackFormConfirmationPartial(ticketId, None))
  }

  private def feedbackConfirmationPage(maybeTicketId: Option[String], loggedIn: Boolean, backUrl: Option[String])(
    implicit request: Request[_]
  ): Result =
    Ok(
      playFrontendFeedbackConfirmationPage(
        backUrl = backUrl
      )
    )

  private def feedbackPage(
    form: Form[FeedbackForm],
    loggedIn: Boolean,
    service: Option[String],
    backUrl: Option[String],
    canOmitComments: Boolean
  )(implicit
    request: Request[_]
  ): Html = {
    val action =
      if (loggedIn) routes.FeedbackController.submit(service, backUrl, canOmitComments)
      else routes.FeedbackController.submitUnauthenticated(service, backUrl, canOmitComments)
    playFrontendFeedbackPage(
      form,
      action
    )
  }
}

object FeedbackFormBind {

  import model.FeedbackFormConfig._

  private val emailValidator                     = new DeskproEmailValidator()
  private val validateEmail: (String) => Boolean = emailValidator.validate

  def emptyForm(
    csrfToken: String,
    referrer: Option[String] = None,
    backUrl: Option[String],
    canOmitComments: Boolean,
    service: Option[String],
    abFeatures: Option[String] = None
  )(implicit
    request: Request[AnyRef]
  ) =
    FeedbackFormBind.form.fill(
      FeedbackForm(
        experienceRating = None,
        name = "",
        email = "",
        comments = "",
        javascriptEnabled = false,
        referrer.getOrElse(request.headers.get(REFERER).getOrElse("n/a")),
        csrfToken,
        Some(service.getOrElse("unknown")),
        abFeatures,
        backUrl,
        canOmitComments
      )
    )

  def form =
    Form[FeedbackForm](
      mapping(
        "feedback-rating"   -> optional(text)
          .verifying("feedback.rating.error.required", rating => rating.isDefined && !rating.get.trim.isEmpty)
          .verifying(
            "feedback.rating.error.invalid",
            rating => rating.map(validExperiences.contains(_)).getOrElse(true)
          ),
        "feedback-name"     -> text
          .verifying("feedback.name.error.required", name => !name.trim.isEmpty)
          .verifying("feedback.name.error.length", name => name.size <= 70),
        "feedback-email"    -> text
          .verifying("feedback.email.error.invalid", validateEmail)
          .verifying("feedback.email.error.length", email => email.size <= 255),
        "feedback-comments" -> FieldMapping[String]()(new Formatter[String] {

          override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
            val commentsCanBeOmitted = data.get("canOmitComments").contains("true")
            data.get(key) match {
              case Some(value) if !value.trim.isEmpty || commentsCanBeOmitted => Right(value.trim)
              case Some(_)                                                    => Left(Seq(FormError(key, "feedback.comments.error.required", Nil)))
              case None                                                       => Left(Seq(FormError(key, "error.required", Nil)))
            }
          }

          override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)

        }).verifying(
          "feedback.comments.error.length",
          comment => {
            val result = comment.size <= 2000
            result
          }
        ),
        "isJavascript"    -> boolean,
        "referrer"        -> text,
        "csrfToken"       -> text,
        "service"         -> optional(text),
        "abFeatures"      -> optional(text),
        "backUrl"         -> optional(text),
        "canOmitComments" -> boolean
      )(FeedbackForm.apply)(FeedbackForm.unapply)
    )
}

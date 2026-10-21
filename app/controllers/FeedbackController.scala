/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.enrolments.EnrolmentsConnector
import model.Aliases.*
import model.FeedbackForm
import play.api.Logging
import play.api.data.Forms.*
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.filters.csrf.CSRF
import play.twirl.api.Html
import services.DeskproSubmission
import uk.gov.hmrc.hmrcfrontend.config.ServiceNavigationConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{BackUrlValidator, DeskproEmailValidator, NameValidator, RefererHeaderRetriever}
import util.ServiceNavigationParamBinder.bindServiceNavigationParam
import views.html.{FeedbackConfirmationPage, FeedbackPage, InternalErrorPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton class FeedbackController @Inject() (
  val ticketQueueConnector: DeskproTicketQueueConnector,
  enrolmentsConnector: EnrolmentsConnector,
  val accessibleUrlValidator: BackUrlValidator,
  mcc: MessagesControllerComponents,
  feedbackConfirmationPage: FeedbackConfirmationPage,
  feedbackPage: FeedbackPage,
  errorPage: InternalErrorPage,
  headerRetriever: RefererHeaderRetriever,
  feedbackFormBind: FeedbackFormBind
)(using AppConfig, ExecutionContext, ServiceNavigationConfig)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport
    with Logging {

  given lang(using request: Request[?]): Lang = request.lang

  val formId = "FeedbackForm"

  def index(
    service: Option[String],
    backUrl: Option[BackUrl],
    canOmitComments: Boolean,
    referrerUrl: Option[ReferrerUrl]
  ): Action[AnyContent] =
    Action.async { request =>
      given Request[AnyContent] = request
      val referrer              = referrerUrl orElse headerRetriever.refererFromHeaders()
      Future.successful(
        Ok(
          renderFeedbackPage(
            feedbackFormBind.emptyForm(
              CSRF.getToken(request).map(_.value).getOrElse(""),
              referrer = referrer,
              backUrl = backUrl,
              canOmitComments = canOmitComments,
              service = service
            ),
            service,
            backUrl,
            canOmitComments = canOmitComments,
            referrer
          )
        )
      )
    }

  def submit(
    service: Option[String] = None,
    backUrl: Option[BackUrl] = None,
    canOmitComments: Boolean = false,
    referrerUrl: Option[ReferrerUrl] = None
  ): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    feedbackFormBind.form
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(feedbackView(formWithError))),
        data =>
          {
            for {
              maybeEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
              _               <- createDeskproFeedback(data, maybeEnrolments)
            } yield Redirect(routes.FeedbackController.thanks(data.backUrl).bindServiceNavigationParam)
          }.recover { case NonFatal(_) =>
            logger.error("Creating feedback ticket failed")
            InternalServerError(errorPage())
          }
      )
  }

  def thanks(backUrl: Option[BackUrl] = None): Action[AnyContent] = Action.async { request =>
    given Request[AnyContent] = request
    val validatedBackUrl      = backUrl.filter(accessibleUrlValidator.validate)
    Future.successful(Ok(feedbackConfirmationPage(backUrl = validatedBackUrl)))
  }

  private def feedbackView(form: Form[FeedbackForm])(using Request[AnyRef]) =
    renderFeedbackPage(
      form,
      form("service").value,
      form("backUrl").value,
      form("canOmitComments").value.contains("true"),
      form("referrer").value
    )

  private def renderFeedbackPage(
    form: Form[FeedbackForm],
    service: Option[String],
    backUrl: Option[BackUrl],
    canOmitComments: Boolean,
    referrerUrl: Option[String]
  )(using Request[?]): Html = {
    val action =
      routes.FeedbackController.submit(service, backUrl, canOmitComments, referrerUrl).bindServiceNavigationParam
    feedbackPage(form, action)
  }
}

class FeedbackFormBind @Inject() (implicit
  emailValidator: DeskproEmailValidator,
  nameValidator: NameValidator
) {
  import model.FeedbackFormConfig.*

  def emptyForm(
    csrfToken: String,
    referrer: Option[String] = None,
    backUrl: Option[BackUrl],
    canOmitComments: Boolean,
    service: Option[String]
  )(using Messages, AppConfig): Form[FeedbackForm] =
    form.fill(
      FeedbackForm(
        experienceRating = None,
        name = "",
        email = "",
        comments = "",
        javascriptEnabled = false,
        referrer.getOrElse("n/a"),
        csrfToken,
        Some(service.getOrElse("unknown")),
        backUrl,
        canOmitComments
      )
    )

  def form(using messages: Messages, appConfig: AppConfig): Form[FeedbackForm] = {
    import appConfig._

    Form[FeedbackForm](
      mapping(
        "feedback-rating"   -> optional(text)
          .verifying("feedback.rating.error.required", rating => rating.isDefined && !rating.get.trim.isEmpty)
          .verifying(
            "feedback.rating.error.invalid",
            rating => rating.forall(validExperiences.contains(_))
          ),
        "feedback-name"     -> text
          .verifying("feedback.name.error.required", name => name.trim.nonEmpty)
          .verifying("forms.name.error.invalid", name => nameValidator.validate(name))
          .verifying(
            messages("feedback.name.error.length", maxNameLength.toString),
            name => name.length <= maxNameLength
          ),
        "feedback-email"    -> text
          .verifying(
            messages("feedback.email.error.length", maxEmailLength.toString),
            email => email.length <= maxEmailLength
          )
          .verifying(
            "feedback.email.error.invalid",
            email =>
              // Do not display the regex validation error if already displayed a length error
              email.length > maxEmailLength || email.trim.isEmpty || emailValidator.validate(email)
          ),
        "feedback-comments" -> FieldMapping[String]()(new Formatter[String] {

          override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
            val commentsCanBeOmitted = data.get("canOmitComments").contains("true")
            data.get(key) match {
              case Some(value) if value.trim.nonEmpty || commentsCanBeOmitted => Right(value.trim)
              case Some(_)                                                    => Left(Seq(FormError(key, "feedback.comments.error.required", Nil)))
              case None                                                       => Left(Seq(FormError(key, "error.required", Nil)))
            }
          }

          override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)

        }).verifying(
          messages("feedback.comments.error.length", maxSingleTextboxLength.toString),
          comment => {
            val result = comment.size <= maxSingleTextboxLength
            result
          }
        ),
        "isJavascript"    -> boolean,
        "referrer"        -> text,
        "csrfToken"       -> text,
        "service"         -> optional(text),
        "backUrl"         -> optional(text),
        "canOmitComments" -> boolean
      )(FeedbackForm.apply)(o => Some(Tuple.fromProductTyped(o)))
    )
  }
}

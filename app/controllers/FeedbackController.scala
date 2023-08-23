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

import javax.inject.{Inject, Singleton}
import model.FeedbackForm
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{MessagesControllerComponents, Request}
import play.filters.csrf.CSRF
import services.DeskproSubmission
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.{BackUrlValidator, DeskproEmailValidator, FeatureFlagSupport, NameValidator, RefererHeaderRetriever}
import views.html.partials.{feedback_form, feedback_form_confirmation}
import views.html.{FeedbackConfirmationPage, FeedbackPage, InternalErrorPage}
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

@Singleton class FeedbackController @Inject() (
  val ticketQueueConnector: DeskproTicketQueueConnector,
  enrolmentsConnector: EnrolmentsConnector,
  val accessibleUrlValidator: BackUrlValidator,
  mcc: MessagesControllerComponents,
  feedbackConfirmationPage: FeedbackConfirmationPage,
  feedbackFormPartial: feedback_form,
  feedbackFormConfirmationPartial: feedback_form_confirmation,
  feedbackPage: FeedbackPage,
  errorPage: InternalErrorPage,
  headerRetriever: RefererHeaderRetriever
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with DeskproSubmission
    with I18nSupport
    with FeatureFlagSupport {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  val formId = "FeedbackForm"

  def index(service: Option[String], backUrl: Option[String], canOmitComments: Boolean, referrerUrl: Option[String]) =
    Action.async { implicit request =>
      val referrer = referrerUrl orElse headerRetriever.refererFromHeaders
      Future.successful(
        Ok(
          renderFeedbackPage(
            FeedbackFormBind.emptyForm(
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
    backUrl: Option[String] = None,
    canOmitComments: Boolean = false,
    referrerUrl: Option[String] = None
  ) = Action.async { implicit request =>
    FeedbackFormBind.form
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(feedbackView(formWithError))),
        data =>
          {
            for {
              maybeEnrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
              _               <- createDeskproFeedback(data, maybeEnrolments)
            } yield Redirect(routes.FeedbackController.thanks(data.backUrl))
          }.recover { case _ =>
            InternalServerError(errorPage())
          }
      )
  }

  def thanks(backUrl: Option[String] = None) = Action.async { implicit request =>
    val validatedBackUrl = backUrl.filter(accessibleUrlValidator.validate)
    Future.successful(Ok(feedbackConfirmationPage(backUrl = validatedBackUrl)))
  }

  def partialIndex(
    submitUrl: String,
    csrfToken: String,
    service: Option[String],
    referer: Option[String],
    canOmitComments: Boolean,
    referrerUrl: Option[String]
  ) = Action.async { implicit request =>
    Future.successful {
      ifPartialsEnabled {
        Ok(
          feedbackFormPartial(
            FeedbackFormBind.emptyForm(
              csrfToken,
              referrerUrl orElse referer orElse headerRetriever.refererFromHeaders,
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
  }

  def partialSubmit(resubmitUrl: String) = Action.async { implicit request =>
    val form = FeedbackFormBind.form.bindFromRequest()
    form.fold(
      error =>
        Future.successful(
          BadRequest(
            feedbackFormPartial(error, resubmitUrl, canOmitComments = form("canOmitComments").value.contains("true"))
          )
        ),
      data =>
        (for {
          enrolments <- enrolmentsConnector.maybeAuthenticatedUserEnrolments()
          ticketId   <- createDeskproFeedback(data, enrolments)
        } yield Ok(ticketId.ticket_id.toString)).recover { case _ =>
          InternalServerError
        }
    )
  }

  def partialThanks(ticketId: String) = Action { implicit request =>
    Ok(feedbackFormConfirmationPartial(ticketId, None))
  }

  private def feedbackView(form: Form[FeedbackForm])(implicit request: Request[AnyRef]) =
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
    backUrl: Option[String],
    canOmitComments: Boolean,
    referrerUrl: Option[String]
  )(implicit
    request: Request[_]
  ): Html = {
    val action = routes.FeedbackController.submit(service, backUrl, canOmitComments, referrerUrl)
    feedbackPage(form, action)
  }
}

object FeedbackFormBind {

  import model.FeedbackFormConfig._

  private val emailValidator = DeskproEmailValidator()
  private val nameValidator  = NameValidator()

  def emptyForm(
    csrfToken: String,
    referrer: Option[String] = None,
    backUrl: Option[String],
    canOmitComments: Boolean,
    service: Option[String]
  ): Form[FeedbackForm] =
    FeedbackFormBind.form.fill(
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
          .verifying("feedback.name.error.required", name => name.trim.nonEmpty)
          .verifying("forms.name.error.invalid", name => nameValidator.validate(name))
          .verifying("feedback.name.error.length", name => name.length <= 70),
        "feedback-email"    -> text
          .verifying("feedback.email.error.invalid", email => emailValidator.validate(email))
          .verifying("feedback.email.error.length", email => email.length <= 255),
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
        "backUrl"         -> optional(text),
        "canOmitComments" -> boolean
      )(FeedbackForm.apply)(FeedbackForm.unapply)
    )
}

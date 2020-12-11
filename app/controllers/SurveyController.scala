/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import javax.inject.{Inject, Singleton}
import config.AppConfig
import model.{SurveyForm, SurveyFormFields}
import play.api.Logging
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Call, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{SurveyConfirmationPage, SurveyPage, survey, survey_confirmation}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SurveyController @Inject() (
  auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  assetsFrontendSurveyPage: survey,
  playFrontendSurveyPage: SurveyPage,
  assetsFrontendSurveyConfirmationPage: survey_confirmation,
  playFrontendSurveyConfirmationPage: SurveyConfirmationPage
)(implicit appConfig: AppConfig, executionContext: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  private val TicketId = "^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$".r

  def validateTicketId(ticketId: String) = ticketId match {
    case TicketId() => true
    case _          => false
  }

  def survey(ticketId: String, serviceId: String) = Action.async { implicit request =>
    Future.successful(
      if (validateTicketId(ticketId)) {
        val form   = emptyForm(serviceId = Some(serviceId), ticketId = Some(ticketId))
        val action = routes.SurveyController.submit
        Ok(surveyPage(form, action))
      } else {
        logger.error(s"Invalid ticket id $ticketId when requesting survey form")
        BadRequest("Invalid ticket id")
      }
    )
  }

  def submit() = Action.async { implicit request =>
    Future.successful {
      if (appConfig.enablePlayFrontendSurveyForm) {
        playFrontendSurveyForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(
              playFrontendSurveyPage(
                formWithErrors,
                routes.SurveyController.submit
              )
            ),
          _ => submitSurveyAction
        )
      } else {
        submitSurveyAction
      }
    }
  }

  def confirmation() = Action.async { implicit request =>
    Future.successful(
      Ok(surveyConfirmationPage)
    )
  }

  private[controllers] def getAuditEventOrFormErrors(implicit
    request: Request[_]
  ): Either[Option[Future[DataEvent]], Seq[FormError]] = {
    val form = surveyForm.bindFromRequest()
    form.errors match {
      case Nil   => Left(Try(form.value.map(buildAuditEvent)).toOption.flatten)
      case e @ _ => Right(e)
    }
  }

  private[controllers] def submitSurveyAction(implicit request: Request[_]): Result = {
    getAuditEventOrFormErrors match {
      case Left(eventOption) =>
        eventOption foreach { dataEventFuture =>
          dataEventFuture.foreach(auditConnector.sendEvent)
        }
      case Right(errors)     =>
        errors foreach { error =>
          logger.error(
            s"Error processing survey form field: '${error.key}' message='${error.message}' args='${error.args.mkString(",")}'"
          )
        }
    }
    Redirect(routes.SurveyController.confirmation())
  }

  private[controllers] def buildAuditEvent(formData: SurveyForm)(implicit hc: HeaderCarrier): Future[DataEvent] =
    Future.successful(
      DataEvent(
        auditSource = "frontend",
        auditType = "DeskproSurvey",
        tags = hc.headers.toMap,
        detail = formData.toStringMap
      )
    )

  private val ratingScale = optional(number(min = 1, max = 5, strict = false))

  private[controllers] def surveyForm = Form[SurveyForm](
    mapping(
      SurveyFormFields.helpful   -> ratingScale,
      SurveyFormFields.speed     -> ratingScale,
      SurveyFormFields.improve   -> optional(text(maxLength = 2500)),
      SurveyFormFields.ticketId  -> optional(text).verifying(ticketId => validateTicketId(ticketId.getOrElse(""))),
      SurveyFormFields.serviceId -> optional(text(maxLength = 20)).verifying(serviceId =>
        serviceId.getOrElse("").length > 0
      )
    )(SurveyForm.apply)(SurveyForm.unapply)
  )

  private[controllers] def playFrontendSurveyForm = Form[SurveyForm](
    mapping(
      SurveyFormFields.helpful   -> ratingScale
        .verifying("survey.helpful.error.required", helpful => helpful.isDefined),
      SurveyFormFields.speed     -> ratingScale
        .verifying("survey.speed.error.required", speed => speed.isDefined),
      SurveyFormFields.improve   -> optional(text)
        .verifying("survey.improve.error.length", improve => improve.getOrElse("").length <= 2500),
      SurveyFormFields.ticketId  -> optional(text),
      SurveyFormFields.serviceId -> optional(text)
    )(SurveyForm.apply)(SurveyForm.unapply)
  )

  private[controllers] def emptyForm(
    serviceId: Option[String] = None,
    ticketId: Option[String] = None
  ): Form[SurveyForm] =
    surveyForm.fill(
      SurveyForm(
        helpful = None,
        speed = None,
        improve = None,
        ticketId = ticketId,
        serviceId = serviceId
      )
    )

  private def surveyPage(form: Form[SurveyForm], action: Call)(implicit
    request: Request[_],
    lang: Lang
  ): Html =
    if (appConfig.enablePlayFrontendSurveyForm) {
      playFrontendSurveyPage(
        form,
        action
      )
    } else {
      assetsFrontendSurveyPage(ticketId = form("ticket-id").value.get, serviceId = form("service-id").value.get)
    }

  private def surveyConfirmationPage(implicit
    request: Request[_],
    lang: Lang
  ): Html =
    if (appConfig.enablePlayFrontendSurveyForm) {
      playFrontendSurveyConfirmationPage()
    } else {
      assetsFrontendSurveyConfirmationPage()
    }
}

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

import javax.inject.{Inject, Singleton}
import config.AppConfig
import model.SurveyForm
import play.api.Logging
import play.api.data.Forms.*
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{SurveyConfirmationPage, SurveyPage}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SurveyController @Inject() (
  auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  playFrontendSurveyPage: SurveyPage,
  playFrontendSurveyConfirmationPage: SurveyConfirmationPage
)(using AppConfig, ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  given lang(using request: Request[?]): Lang = request.lang

  private val TicketId = "^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$".r

  // TODO: These header names have been copied from http-verbs after v13.0.0 of that library removed the call to get all
  // headers without parameters. Ticket to assess what headers actually required: PLATUI-1111
  private val allHeaderNames = Seq(
    HeaderNames.xRequestId,
    HeaderNames.xSessionId,
    HeaderNames.xForwardedFor,
    HeaderNames.xRequestChain,
    HeaderNames.authorisation,
    HeaderNames.trueClientIp,
    HeaderNames.trueClientPort,
    HeaderNames.googleAnalyticTokenId,
    HeaderNames.googleAnalyticUserId,
    HeaderNames.deviceID,
    HeaderNames.akamaiReputation
  )

  def validateTicketId(ticketId: String): Boolean = ticketId match {
    case TicketId() => true
    case _          => false
  }

  def survey(ticketId: String, serviceId: String): Action[AnyContent] = Action.asyncUsing { request ?=>
    Future.successful(
      if (validateTicketId(ticketId)) {
        val form   = emptyForm(serviceId = Some(serviceId), ticketId = Some(ticketId))
        val action = routes.SurveyController.submit(ticketId, serviceId)
        Ok(surveyPage(form, action))
      } else {
        logger.error(s"Invalid ticket id $ticketId when requesting survey form")
        BadRequest("Invalid ticket id")
      }
    )
  }

  def submitDeprecated(): Action[AnyContent] = Action.asyncUsing { request ?=>
    Future.successful {
      submitSurveyAction
    }
  }

  def submit(ticketId: String, serviceId: String): Action[AnyContent] = Action.asyncUsing { request ?=>
    Future.successful {
      playFrontendSurveyForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            BadRequest(
              playFrontendSurveyPage(
                formWithErrors,
                routes.SurveyController.submit(ticketId, serviceId)
              )
            ),
          _ => submitSurveyAction
        )
    }
  }

  def confirmation(): Action[AnyContent] = Action.asyncUsing { request ?=>
    Future.successful(
      Ok(surveyConfirmationPage)
    )
  }

  private[controllers] def getAuditEventOrFormErrors(using
    Request[?]
  ): Either[Option[Future[DataEvent]], Seq[FormError]] = {
    val form = surveyForm.bindFromRequest()
    form.errors match {
      case Nil   => Left(Try(form.value.map(buildAuditEvent)).toOption.flatten)
      case e @ _ => Right(e)
    }
  }

  private[controllers] def submitSurveyAction(using request: Request[?]): Result = {
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

  private[controllers] def buildAuditEvent(formData: SurveyForm)(using hc: HeaderCarrier): Future[DataEvent] =
    Future.successful(
      DataEvent(
        auditSource = "frontend",
        auditType = "DeskproSurvey",
        tags = hc.headers(names = allHeaderNames).toMap,
        detail = collection.immutable.HashMap(
          "helpful"   -> formData.helpful.getOrElse(0).toString,
          "speed"     -> formData.speed.getOrElse(0).toString,
          "improve"   -> formData.improve.getOrElse(""),
          "ticketId"  -> formData.ticketId.getOrElse(""),
          "serviceId" -> formData.serviceId.getOrElse("")
        )
      )
    )

  private val ratingScale = optional(number(min = 1, max = 5, strict = false))

  private[controllers] def surveyForm = Form[SurveyForm](
    mapping(
      "helpful"    -> ratingScale,
      "speed"      -> ratingScale,
      "improve"    -> optional(text(maxLength = 2500)),
      "ticket-id"  -> optional(text).verifying(ticketId => validateTicketId(ticketId.getOrElse(""))),
      "service-id" -> optional(text(maxLength = 20)).verifying(serviceId => serviceId.getOrElse("").length > 0)
    )(SurveyForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  private[controllers] def playFrontendSurveyForm = Form[SurveyForm](
    mapping(
      "helpful"    -> ratingScale
        .verifying("survey.helpful.error.required", helpful => helpful.isDefined),
      "speed"      -> ratingScale
        .verifying("survey.speed.error.required", speed => speed.isDefined),
      "improve"    -> optional(text)
        .verifying("survey.improve.error.length", improve => improve.getOrElse("").length <= 2500),
      "ticket-id"  -> optional(text),
      "service-id" -> optional(text)
    )(SurveyForm.apply)(o => Some(Tuple.fromProductTyped(o)))
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

  private def surveyPage(form: Form[SurveyForm], action: Call)(using Request[?]): Html =
    playFrontendSurveyPage(form, action)

  private def surveyConfirmationPage(using Request[?]): Html =
    playFrontendSurveyConfirmationPage()
}

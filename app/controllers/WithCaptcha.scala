/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers

import config.AppConfig
import javax.inject.Inject
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result, Results}
import services.CaptchaService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.InternalErrorPage
import views.html.helpers.recaptcha

import scala.concurrent.{ExecutionContext, Future}

abstract class WithCaptcha @Inject() (
  mcc: MessagesControllerComponents,
  errorPage: InternalErrorPage,
  recaptcha: recaptcha
) extends FrontendController(mcc)
    with Results
    with I18nSupport
    with Logging {

  implicit val appConfig: AppConfig

  implicit val captchaService: CaptchaService

  implicit val executionContext: ExecutionContext

  implicit def lang(implicit request: Request[_]): Lang = request.lang

  case class Recaptcha(response: String)

  val recaptchaForm = Form[Recaptcha](
    mapping(
      "recaptcha-v3-response" -> text
    )(Recaptcha.apply)(Recaptcha.unapply)
  )

  def recaptchaFormComponent(action: String) =
    recaptcha(appConfig.captchaClientKey, action, appConfig.captchaEnabled)

  protected def validateCaptcha[R <: Result](request: Request[AnyContent])(action: => Future[R]) = {

    implicit val implicitRequest = request

    if (appConfig.captchaEnabled) {
      recaptchaForm.bindFromRequest()(request) fold (
        errors => {
          logger.warn(s"Part of the POST request responsible for captcha is malformed. Errors $errors")
          Future.successful(BadRequest(errorPage()))
        },
        form => {
          for {
            isValid <- captchaService.validateCaptcha(form.response)
            result  <- if (isValid) {
                         action
                       } else {
                         logger.warn("There was a failed captcha result")
                         Future.successful(BadRequest(errorPage()))
                       }
          } yield result
        }
      )
    } else {
      action
    }
  }

}

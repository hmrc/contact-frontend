package controllers

import config.AppConfig
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, Request, Result, Results}
import services.CaptchaService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.deskpro_error

import scala.concurrent.Future

trait WithCaptcha extends Results with FrontendController with I18nSupport {

  implicit val appConfig : AppConfig

  implicit val captchaService : CaptchaService

  case class Recaptcha(response : String)

  val recaptchaForm = Form[Recaptcha](
    mapping(
      "recaptcha-v3-response" -> text
    )(Recaptcha.apply)(Recaptcha.unapply)
  )

  def recaptchaFormComponent(action : String) = {
    views.html.helpers.recaptcha(appConfig.captchaClientKey, action, appConfig.captchaEnabled)
  }

  protected def validateCaptcha[R <: Result](request : Request[AnyContent])(action :  => Future[R]) = {

    implicit val implicitRequest = request

    if (appConfig.captchaEnabled) {
      recaptchaForm.bindFromRequest()(request) fold(
        errors => {
          Logger.warn(s"Part of the POST request responsible for captcha is malformed. Errors ${errors}")
          Future.successful(BadRequest(deskpro_error()))
        },
        form => {
          for {
            isValid <- captchaService.validateCaptcha(form.response)
            result <- if (isValid) {
              action
            } else {
              Logger.warn("There was a failed captcha result")
              Future.successful(BadRequest(deskpro_error()))
            }
          } yield result
        }
      )
    } else {
      action
    }

  }


}

/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package testonly.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import config.AppConfig
import play.api.i18n.{I18nSupport, Lang}
import testonly.views.html.TestPage

import scala.concurrent.Future

@Singleton
class TestController @Inject() (appConfig: AppConfig, mcc: MessagesControllerComponents, testPage: TestPage)
    extends FrontendController(mcc)
    with I18nSupport {

  implicit val config: AppConfig                        = appConfig
  implicit def lang(implicit request: Request[_]): Lang = request.lang

  val test: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(testPage()))
  }
}

/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import javax.inject.{Inject, Singleton}
import config.AppConfig
import play.api.Logging
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.LanguageUtils

@Singleton
class LanguageController @Inject()(mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with Logging {

  val english = Lang("en")
  val welsh   = Lang("cy")

  def switchToEnglish: Action[AnyContent] = switchToLang(english)

  def switchToWelsh: Action[AnyContent] = switchToLang(welsh)

  private def switchToLang(lang: Lang): Action[AnyContent] = Action { implicit request =>
    val newLang = if (appConfig.enableLanguageSwitching) lang else english

    request.headers.get(REFERER) match {
      case Some(referrer) => Redirect(referrer).withLang(newLang).flashing(LanguageUtils.flashWithSwitchIndicator)
      case None => {
        logger.warn(s"Unable to get the referrer, so sending them to ${appConfig.fallbackURLForLangugeSwitcher}")
        Redirect(appConfig.fallbackURLForLangugeSwitcher).withLang(newLang)
      }
    }
  }

}

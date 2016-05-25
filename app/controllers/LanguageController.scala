package controllers

import config.CFConfig
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Lang
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import util.LanguageUtils

trait LanguageController extends FrontendController {

    val english = Lang("en")
    val welsh = Lang("cy")

  def switchToEnglish = switchToLang(english)

  def switchToWelsh = switchToLang(welsh)
  
  private def switchToLang(lang: Lang) = Action { implicit request =>
    val newLang = if (CFConfig.enableLanguageSwitching) lang else english

    request.headers.get(REFERER) match {
      case Some(referrer) => Redirect(referrer).withLang(newLang).flashing(LanguageUtils.flashWithSwitchIndicator)
      case None => {
          Logger.warn(s"Unable to get the referrer, so sending them to ${CFConfig.fallbackURLForLangugeSwitcher}")
          Redirect(CFConfig.fallbackURLForLangugeSwitcher).withLang(newLang)
        }
    }
  }
  
}


object LanguageController extends LanguageController
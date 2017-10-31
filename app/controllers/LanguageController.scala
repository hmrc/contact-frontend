package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController
import util.LanguageUtils

@Singleton
class LanguageController @Inject() (override val messagesApi: MessagesApi)(implicit appConfig : AppConfig) extends FrontendController with I18nSupport{

    val english = Lang("en")
    val welsh = Lang("cy")

  def switchToEnglish = switchToLang(english)

  def switchToWelsh = switchToLang(welsh)
  
  private def switchToLang(lang: Lang) = Action { implicit request =>
    val newLang = if (appConfig.enableLanguageSwitching) lang else english

    request.headers.get(REFERER) match {
      case Some(referrer) => Redirect(referrer).withLang(newLang).flashing(LanguageUtils.flashWithSwitchIndicator)
      case None => {
          Logger.warn(s"Unable to get the referrer, so sending them to ${appConfig.fallbackURLForLangugeSwitcher}")
          Redirect(appConfig.fallbackURLForLangugeSwitcher).withLang(newLang)
        }
    }
  }
  
}

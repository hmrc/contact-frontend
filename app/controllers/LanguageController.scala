/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import com.google.inject.Inject
import config.AppConfig
import javax.inject.Singleton
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc._
import uk.gov.hmrc.play.language.{LanguageController => PlayLanguageController, LanguageUtils}

@Singleton
case class LanguageController @Inject() (
  configuration: Configuration,
  languageUtils: LanguageUtils,
  cc: ControllerComponents,
  appConfig: AppConfig
) extends PlayLanguageController(configuration, languageUtils, cc) {
  import appConfig._

  def switchToEnglish: Action[AnyContent] = switchToLanguage(en)

  def switchToWelsh: Action[AnyContent] = switchToLanguage(cy)

  override def fallbackURL: String = appConfig.fallbackURLForLanguageSwitcher

  override protected def languageMap: Map[String, Lang] =
    Map(en -> Lang(en), cy -> Lang(cy))
}

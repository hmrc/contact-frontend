/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package helpers

import config.AppConfig
import play.api.mvc.Request
import util.Feature

class TestAppConfig extends AppConfig {

  def assetsPrefix: String = ???

  def analyticsToken: String = "token"

  def analyticsHost: String = "localhost"

  def externalReportProblemUrl: String = ???

  def externalReportProblemSecureUrl: String = ???

  def backUrlDestinationWhitelist: Set[String] = ???

  def loginCallback(continueUrl: String): String = ???

  def fallbackURLForLanguageSwitcher: String = ???

  def enableLanguageSwitching: Boolean = true

  def enablePlayFrontendAccessibilityForm: Boolean = false

  def enablePlayFrontendFeedbackForm: Boolean = false

  def enablePlayFrontendProblemReportNonjsForm: Boolean = false

  def enablePlayFrontendSurveyForm: Boolean = false

  def captchaEnabled: Boolean = ???

  def captchaMinScore: BigDecimal = ???

  def captchaClientKey: String = ???

  def captchaServerKey: String = ???

  def captchaVerifyUrl: String = ???

  def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean = ???

  def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature] = ???

}

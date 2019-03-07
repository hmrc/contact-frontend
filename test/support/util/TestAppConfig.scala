package support.util

import config.AppConfig
import play.api.mvc.Request
import util.Feature

class TestAppConfig extends AppConfig {

  override def assetsPrefix: String = ???

  override def analyticsToken: String = "token"

  override def analyticsHost: String = "localhost"

  override def externalReportProblemUrl: String = ???

  override def externalReportProblemSecureUrl: String = ???

  override def backUrlDestinationWhitelist: Set[String] = ???

  override def loginCallback(continueUrl: String): String = ???

  override def fallbackURLForLangugeSwitcher: String = ???

  override def enableLanguageSwitching: Boolean = true

  override def captchaEnabled: Boolean = ???

  override def captchaMinScore: BigDecimal = ???

  override def captchaClientKey: String = ???

  override def captchaServerKey: String = ???

  override def captchaVerifyUrl: String = ???

  override def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean = ???

  override def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature] = ???
}

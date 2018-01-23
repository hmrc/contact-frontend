package config

import javax.inject.Inject

import play.api.Configuration
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val externalReportProblemUrl: String
  val externalReportProblemSecureUrl: String
  val returnUrlHostnameWhitelist: Set[String]
  def loginCallback(continueUrl: String): String
  def fallbackURLForLangugeSwitcher: String
  def enableLanguageSwitching: Boolean
  def useV2ProblemReportAjax: Boolean
}

class CFConfig @Inject() (environment: play.api.Environment, configuration : Configuration) extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactHost = configuration.getString(s"govuk-tax.$env.contact-frontend.host").getOrElse("")

  override lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  override lazy val externalReportProblemSecureUrl = s"$contactHost/contact/problem_reports_secure"
  override lazy val assetsPrefix = loadConfig(s"frontend.assets.url") + loadConfig(s"frontend.assets.version")
  override lazy val analyticsToken = loadConfig(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"govuk-tax.$env.google-analytics.host")
  override lazy val returnUrlHostnameWhitelist = loadConfig(s"$env.backUrlHostnameWhitelist").split(',').filter(_.nonEmpty).toSet
  override def loginCallback(continueUrl: String) = s"$contactHost$continueUrl"
  override def fallbackURLForLangugeSwitcher = loadConfig(s"govuk-tax.$env.platform.frontend.url")
  override def enableLanguageSwitching = configuration.getBoolean(s"govuk-tax.$env.enableLanguageSwitching").getOrElse(false)
  override def useV2ProblemReportAjax: Boolean = configuration.getBoolean("feature-flag.v2problemreportajax").getOrElse(false)

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration


}

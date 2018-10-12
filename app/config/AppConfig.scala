package config

import javax.inject.Inject
import play.api.Configuration
import play.api.mvc.Request
import uk.gov.hmrc.play.config.ServicesConfig
import util._

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val externalReportProblemUrl: String
  val externalReportProblemSecureUrl: String
  val backUrlDestinationWhitelist: Set[String]
  def loginCallback(continueUrl: String): String
  def fallbackURLForLangugeSwitcher: String
  def enableLanguageSwitching: Boolean

  def hasFeature(f : Feature)(implicit request: Request[_]): Boolean

  def getFeatures(implicit request: Request[_]): Set[Feature]
}

class CFConfig @Inject() (environment: play.api.Environment, configuration : Configuration) extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactHost = configuration.getString(s"govuk-tax.$env.contact-frontend.host").getOrElse("")

  private val getHelpWithThisPageFeatureSplit = configuration.getInt("features.getHelpWithThisPage.split").getOrElse(0)

  override lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  override lazy val externalReportProblemSecureUrl = s"$contactHost/contact/problem_reports_secure"
  override lazy val assetsPrefix = loadConfig(s"frontend.assets.url") + loadConfig(s"frontend.assets.version")
  override lazy val analyticsToken = loadConfig(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"govuk-tax.$env.google-analytics.host")
  override lazy val backUrlDestinationWhitelist = loadConfig(s"$env.backUrlDestinationWhitelist").split(',').filter(_.nonEmpty).toSet
  override def loginCallback(continueUrl: String) = s"$contactHost$continueUrl"
  override def fallbackURLForLangugeSwitcher = loadConfig(s"govuk-tax.$env.platform.frontend.url")
  override def enableLanguageSwitching = configuration.getBoolean(s"govuk-tax.$env.enableLanguageSwitching").getOrElse(false)

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  lazy val getHelpWithThisPageFeaturePartitioner: FeaturePartitioner[Request[_], GetHelpWithThisPageFeature] =
    new DeviceIdFeaturePartitionerDecorator(
      DeviceIdProvider.deviceIdProvider,
      new ABTestingFeaturePartitioner(getHelpWithThisPageFeatureSplit,
        GetHelpWithThisPageFeature_A, GetHelpWithThisPageFeature_B))

  override def hasFeature(f : Feature)(implicit request: Request[_]): Boolean = {
    f match {
      case f : GetHelpWithThisPageFeature => getHelpWithThisPageFeaturePartitioner.partition(request) == f
      case _ => false
    }
  }

  override def getFeatures(implicit request: Request[_]): Set[Feature] = {
    val getHelpWithThisPageFeature = getHelpWithThisPageFeaturePartitioner.partition(request)
    Set(getHelpWithThisPageFeature)
  }

}

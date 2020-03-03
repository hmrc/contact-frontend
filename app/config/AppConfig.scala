package config

import javax.inject.Inject
import play.api.Configuration
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util._
import collection.JavaConversions._
import scala.util.Try

trait AppConfig {
  def assetsPrefix: String
  def analyticsToken: String
  def analyticsHost: String
  def externalReportProblemUrl: String
  def externalReportProblemSecureUrl: String
  def backUrlDestinationWhitelist: Set[String]
  def loginCallback(continueUrl: String): String
  def fallbackURLForLangugeSwitcher: String
  def enableLanguageSwitching: Boolean
  def captchaEnabled: Boolean
  def captchaMinScore: BigDecimal
  def captchaClientKey: String
  def captchaServerKey: String
  def captchaVerifyUrl : String

  def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean

  def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature]
}

class CFConfig @Inject()(environment: play.api.Environment,
                         configuration: Configuration,
                         servicesConfig: ServicesConfig)
    extends AppConfig {

  private def loadConfig(key: String) =
    configuration
      .getOptional[String](key)
      .getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactHost = configuration
    .getOptional[String]("contact-frontend.host")
    .getOrElse("")

  private val featureRules: Seq[FeatureEnablingRule] =
    Try(configuration.underlying.getStringList("features"))
      .toOption
      .map(_.toList)
      .getOrElse(List.empty)
      .map(FeatureEnablingRule.parse)

  override lazy val externalReportProblemUrl =
    s"$contactHost/contact/problem_reports"
  override lazy val externalReportProblemSecureUrl =
    s"$contactHost/contact/problem_reports_secure"
  override lazy val assetsPrefix   = loadConfig("frontend.assets.url") + loadConfig(s"frontend.assets.version")
  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost  = loadConfig("google-analytics.host")
  override lazy val backUrlDestinationWhitelist =
    loadConfig("backUrlDestinationWhitelist").split(',').filter(_.nonEmpty).toSet
  override def loginCallback(continueUrl: String) = s"$contactHost$continueUrl"
  override def fallbackURLForLangugeSwitcher =
    loadConfig("platform.frontend.url")
  override def enableLanguageSwitching =
    configuration
      .getOptional[Boolean]("enableLanguageSwitching")
      .getOrElse(false)

  override lazy val captchaEnabled: Boolean = servicesConfig.getBoolean("captcha.v3.enabled")

  override lazy val captchaMinScore: BigDecimal = BigDecimal(servicesConfig.getString("captcha.v3.minScore"))

  override lazy val captchaClientKey: String = servicesConfig.getString("captcha.v3.keys.client")

  override lazy val captchaServerKey: String = servicesConfig.getString("captcha.v3.keys.server")

  override lazy val captchaVerifyUrl : String = servicesConfig.getString("captcha.v3.verifyUrl")

  lazy val featureSelector: FeatureSelector =
    new BucketBasedFeatureSelector(BucketCalculator.deviceIdBucketCalculator, featureRules)

  override def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean =
    featureSelector.computeFeatures(request, service).contains(f)

  override def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature] =
    featureSelector.computeFeatures(request, service)

}

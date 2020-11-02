/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package config

import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Request
import util._

import scala.collection.JavaConverters._
import scala.util.Try

trait AppConfig {
  def assetsPrefix: String
  def analyticsToken: String
  def analyticsHost: String
  def externalReportProblemUrl: String
  def externalReportProblemSecureUrl: String
  def backUrlDestinationWhitelist: Set[String]
  def loginCallback(continueUrl: String): String
  def fallbackURLForLanguageSwitcher: String
  def enableLanguageSwitching: Boolean
  def enablePlayFrontendAccessibilityForm: Boolean
  def captchaEnabled: Boolean
  def captchaMinScore: BigDecimal
  def captchaClientKey: String
  def captchaServerKey: String
  def captchaVerifyUrl: String

  def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean

  def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature]

  val en: String = "en"
  val cy: String = "cy"
}

class CFConfig @Inject() (configuration: Configuration) extends AppConfig {

  private def loadConfigString(key: String): String =
    configuration
      .getOptional[String](key)
      .getOrElse(configNotFoundError(key))

  private val contactHost = configuration
    .getOptional[String]("contact-frontend.host")
    .getOrElse("")

  private val featureRules: Seq[FeatureEnablingRule] =
    Try(configuration.underlying.getStringList("features")).toOption
      .map(_.asScala.toList)
      .getOrElse(List.empty)
      .map(FeatureEnablingRule.parse)

  override lazy val externalReportProblemUrl =
    s"$contactHost/contact/problem_reports"

  override lazy val externalReportProblemSecureUrl =
    s"$contactHost/contact/problem_reports_secure"

  override lazy val assetsPrefix =
    loadConfigString("frontend.assets.url") + loadConfigString(s"frontend.assets.version")

  override lazy val analyticsToken = loadConfigString("google-analytics.token")

  override lazy val analyticsHost = loadConfigString("google-analytics.host")

  override lazy val backUrlDestinationWhitelist =
    loadConfigString("backUrlDestinationWhitelist").split(',').filter(_.nonEmpty).toSet

  override def loginCallback(continueUrl: String) = s"$contactHost$continueUrl"

  override def fallbackURLForLanguageSwitcher =
    loadConfigString("platform.frontend.url")

  override def enableLanguageSwitching =
    configuration
      .getOptional[Boolean]("enableLanguageSwitching")
      .getOrElse(false)

  override def enablePlayFrontendAccessibilityForm =
    configuration
      .getOptional[Boolean]("enablePlayFrontendAccessibilityForm")
      .getOrElse(false)

  override lazy val captchaEnabled: Boolean =
    configuration
      .getOptional[Boolean]("captcha.v3.enabled")
      .getOrElse(configNotFoundError("captcha.v3.enabled"))

  override lazy val captchaMinScore: BigDecimal = BigDecimal(loadConfigString("captcha.v3.minScore"))

  override lazy val captchaClientKey: String = loadConfigString("captcha.v3.keys.client")

  override lazy val captchaServerKey: String = loadConfigString("captcha.v3.keys.server")

  override lazy val captchaVerifyUrl: String = loadConfigString("captcha.v3.verifyUrl")

  lazy val featureSelector: FeatureSelector =
    new BucketBasedFeatureSelector(BucketCalculator.deviceIdBucketCalculator, featureRules)

  override def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean =
    featureSelector.computeFeatures(request, service).contains(f)

  override def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature] =
    featureSelector.computeFeatures(request, service)

  private def configNotFoundError(key: String) =
    throw new RuntimeException(s"Could not find config key '$key'")
}

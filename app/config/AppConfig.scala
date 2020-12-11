/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package config

import javax.inject.Inject
import play.api.Configuration
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
  def enablePlayFrontendFeedbackForm: Boolean
  def enablePlayFrontendProblemReportNonjsForm: Boolean
  def enablePlayFrontendSurveyForm: Boolean
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

  lazy val externalReportProblemUrl =
    s"$contactHost/contact/problem_reports"

  lazy val externalReportProblemSecureUrl =
    s"$contactHost/contact/problem_reports_secure"

  lazy val assetsPrefix =
    loadConfigString("frontend.assets.url") + loadConfigString(s"frontend.assets.version")

  lazy val analyticsToken = loadConfigString("google-analytics.token")

  lazy val analyticsHost = loadConfigString("google-analytics.host")

  lazy val backUrlDestinationWhitelist =
    loadConfigString("backUrlDestinationWhitelist")
      .split(',')
      .filter(_.nonEmpty)
      .toSet

  def loginCallback(continueUrl: String) = s"$contactHost$continueUrl"

  def fallbackURLForLanguageSwitcher =
    loadConfigString("platform.frontend.url")

  def enableLanguageSwitching: Boolean =
    configuration
      .getOptional[Boolean]("enableLanguageSwitching")
      .getOrElse(false)

  def enablePlayFrontendAccessibilityForm: Boolean =
    configuration
      .getOptional[Boolean]("enablePlayFrontendAccessibilityForm")
      .getOrElse(false)

  def enablePlayFrontendFeedbackForm: Boolean =
    configuration
      .getOptional[Boolean]("enablePlayFrontendFeedbackForm")
      .getOrElse(false)

  def enablePlayFrontendProblemReportNonjsForm: Boolean =
    configuration
      .getOptional[Boolean]("enablePlayFrontendProblemReportsForm")
      .getOrElse(false)

  def enablePlayFrontendSurveyForm: Boolean =
    configuration
      .getOptional[Boolean]("enablePlayFrontendSurveyForm")
      .getOrElse(false)

  lazy val captchaEnabled: Boolean =
    configuration
      .getOptional[Boolean]("captcha.v3.enabled")
      .getOrElse(configNotFoundError("captcha.v3.enabled"))

  lazy val captchaMinScore: BigDecimal = BigDecimal(loadConfigString("captcha.v3.minScore"))

  lazy val captchaClientKey: String = loadConfigString("captcha.v3.keys.client")

  lazy val captchaServerKey: String = loadConfigString("captcha.v3.keys.server")

  lazy val captchaVerifyUrl: String = loadConfigString("captcha.v3.verifyUrl")

  lazy val featureSelector: FeatureSelector =
    new BucketBasedFeatureSelector(BucketCalculator.deviceIdBucketCalculator, featureRules)

  def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean =
    featureSelector.computeFeatures(request, service).contains(f)

  def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature] =
    featureSelector.computeFeatures(request, service)

  private def configNotFoundError(key: String) =
    throw new RuntimeException(s"Could not find config key '$key'")

}

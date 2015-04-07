package config

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val homeUrl: String
  val accountDetailsUrl: String
  val helpUrl: String
  val signOutUrl: String
  val externalReportProblemUrl: String
  val externalReportProblemSecureUrl: String
}

object CFConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val ytaHost = configuration.getString(s"govuk-tax.$env.yta.host").getOrElse("")
  private val contactHost = configuration.getString(s"govuk-tax.$env.contact-frontend.host").getOrElse("")

  override lazy val externalReportProblemUrl = s"$contactHost/contact/problem_reports"
  override lazy val externalReportProblemSecureUrl = s"$contactHost/contact/problem_reports_secure"
  override lazy val assetsPrefix = loadConfig(s"govuk-tax.$env.assets.url") + loadConfig(s"govuk-tax.$env.assets.version")
  override lazy val analyticsToken = loadConfig(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"govuk-tax.$env.google-analytics.host")
  override lazy val betaFeedbackUrl = controllers.routes.FeedbackController.feedbackForm().url
  override lazy val betaFeedbackUnauthenticatedUrl = controllers.routes.FeedbackController.unauthenticatedFeedbackForm().url
  override lazy val homeUrl = s"$ytaHost/account"
  override lazy val accountDetailsUrl = s"$homeUrl/account-details"
  override lazy val helpUrl = controllers.routes.ContactHmrcController.index().url
  override lazy val signOutUrl = s"$homeUrl/survey"
}

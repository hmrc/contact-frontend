package controllers

import config.CFConfig
import play.api.Play
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

class GovernmentGatewayAuthProvider(continueUrl: String) extends GovernmentGateway {
  //todo move the URL login to ExternalUrls
  lazy val companyAuthUrl = Play.current.configuration.getString(s"govuk-tax.${RunMode.env}.company-auth.host").getOrElse("")
  override def loginURL: String = s"$companyAuthUrl/gg/sign-in"
  override def continueURL: String = CFConfig.loginCallback(continueUrl)
}
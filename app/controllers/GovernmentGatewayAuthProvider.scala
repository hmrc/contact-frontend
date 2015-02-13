package controllers

import controllers.common.GovernmentGateway
import play.api.Play
import uk.gov.hmrc.play.config.RunMode

class GovernmentGatewayAuthProvider(continueUrl: String) extends GovernmentGateway {
  //todo move the URL login to ExternalUrls
  lazy val companyAuthUrl = Play.current.configuration.getString(s"govuk-tax.${RunMode.env}.company-auth.host").getOrElse("/account")

  override def login: String = s"$companyAuthUrl/sign-in?continue=$continueUrl"
}
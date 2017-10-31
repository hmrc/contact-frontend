package controllers

import javax.inject.Inject

import config.AppConfig
import play.api.Play
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

class GovernmentGatewayAuthProvider @Inject() (appConfig : AppConfig) {

  def forUrl(continueUrl: String): GovernmentGateway = {
    //todo move the URL login to ExternalUrls
    new GovernmentGateway {
      lazy val companyAuthUrl = Play.current.configuration.getString(s"govuk-tax.${RunMode.env}.company-auth.host").getOrElse("")
      override def loginURL: String = s"$companyAuthUrl/gg/sign-in"
      override def continueURL: String = appConfig.loginCallback(continueUrl)
    }
  }

}
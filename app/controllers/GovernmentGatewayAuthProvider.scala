package controllers

import javax.inject.Inject

import config.AppConfig
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

class GovernmentGatewayAuthProvider @Inject() (appConfig : AppConfig, configuration : Configuration) {

  def forUrl(continueUrl: String): GovernmentGateway = {
    //todo move the URL login to ExternalUrls
    new GovernmentGateway {
      lazy val companyAuthUrl = configuration.getString(s"govuk-tax.${RunMode.env}.company-auth.host").getOrElse("")
      override def loginURL: String = s"$companyAuthUrl/gg/sign-in"
      override def continueURL: String = appConfig.loginCallback(continueUrl)
    }
  }

}
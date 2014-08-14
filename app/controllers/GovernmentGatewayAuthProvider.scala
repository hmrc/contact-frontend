package controllers

import controllers.common.GovernmentGateway
import play.api.Play
import uk.gov.hmrc.play.config.RunMode

class GovernmentGatewayAuthProvider(continueUrl: String) extends GovernmentGateway {
  lazy val companyAuthUrl = Play.current.configuration.getString(s"govuk-tax.${RunMode.env}.company-auth.host").getOrElse("")

  override def login: String = s"$companyAuthUrl/sign-in?continue=$continueUrl"
}
package config

import javax.inject.Inject

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class FrontendAuthConnector @Inject() (val http: WSHttp) extends AuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")
}
package config

import javax.inject.Inject

import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

class FrontendAuthConnector @Inject() (val http: WSHttp) extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")
}

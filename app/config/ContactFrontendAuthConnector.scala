package config

import javax.inject.Inject

import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.CorePost
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

class ContactFrontendAuthConnector @Inject()(
                                       httpClient: HttpClient,
                                       override val runModeConfiguration: Configuration,
                                       val environment: Environment
                                     ) extends PlayAuthConnector with ServicesConfig {

  override val serviceUrl: String = baseUrl("auth")

  override def http: CorePost = httpClient

  override protected def mode = environment.mode
}

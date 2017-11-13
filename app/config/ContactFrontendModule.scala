package config

import play.api.inject.Module
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector

class ContactFrontendModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[AppConfig].to[CFConfig]
    )
  }
}


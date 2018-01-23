package config

import play.api.inject.Module
import play.api.{Configuration, Environment}
import util.{BackUrlValidator, ConfigurationBasedBackUrlValidator}

class ContactFrontendModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[AppConfig].to[CFConfig],
      bind[BackUrlValidator].to[ConfigurationBasedBackUrlValidator]
    )
  }
}


/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package config

import play.api.inject.Module
import play.api.{Configuration, Environment}
import services.{CaptchaService, CaptchaServiceV3}
import util.{BackUrlValidator, ConfigurationBasedBackUrlValidator}

class ContactFrontendModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) =
    Seq(
      bind[AppConfig].to[CFConfig],
      bind[BackUrlValidator].to[ConfigurationBasedBackUrlValidator],
      bind[CaptchaService].to[CaptchaServiceV3]
    )
}

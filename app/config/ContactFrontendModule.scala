package config

import play.api.inject.Module
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class ContactFrontendModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[AuthConnector].to[FrontendAuthConnector],
      bind[AuditConnector].to[StaticAuditConnector],
      bind[WSHttp].toSelf,
      bind[AppConfig].to[CFConfig]
    )
  }
}


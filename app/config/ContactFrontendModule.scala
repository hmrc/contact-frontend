package config

import play.api.inject.Module
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

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

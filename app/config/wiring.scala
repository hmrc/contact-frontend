package config

import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig

object AuditConnector extends Auditing with AppName with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = NoneRequired
  override lazy val auditConnector: Auditing = AuditConnector
}

trait WSHttp extends HttpGet with WSGet with HttpPost with WSPost with Hooks with AppName
object WSHttp extends WSHttp

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")
  override def http: CoreGet = WSHttp
}
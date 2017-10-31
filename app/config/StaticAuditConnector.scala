package config

import javax.inject.Singleton

import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig

@Singleton
class StaticAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig: AuditingConfig = LoadAuditingConfig(s"$env.auditing")
}

package config

import javax.inject.Inject

import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.{HttpGet, HttpPost}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

class WSHttp @Inject() (override val auditConnector : StaticAuditConnector)
  extends HttpGet with WSGet with HttpPost with WSPost with HttpHooks with HttpAuditing with AppName {
  override val hooks = NoneRequired
}

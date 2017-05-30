package config

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.{CacheControlFilter, MicroserviceFilterSupport, RecoveryFilter}
import uk.gov.hmrc.play.filters.frontend.HeadersFilter
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter

object ContactFrontendGlobal
  extends DefaultFrontendGlobal
  with RunMode {

  override def auditConnector = AuditConnector
  override def loggingFilter = CFLoggingFilter
  override def frontendAuditFilter = ContactFrontendAuditFilter
  override def frontendFilters = Seq(
    metricsFilter,
    HeadersFilter,
    SessionCookieCryptoFilter,
    deviceIdFilter,
    loggingFilter,
    frontendAuditFilter,
    sessionTimeoutFilter,
    csrfExceptionsFilter,
    csrfFilter,
    CacheControlFilter.fromConfig("caching.allowedContentTypes"),
    RecoveryFilter,
    CorsFilter
  )

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    views.html.error_template(pageTitle, heading, message)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] =
    app.configuration.getConfig(s"$env.microservice.metrics")

}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object CFLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object ContactFrontendAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {

  override lazy val maskedFormFields = Seq.empty

  override lazy val applicationPort = None

  override lazy val auditConnector = AuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}


package config

import java.io.File

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode._
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.auth.controllers.AuthParamsConfigurationValidator
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.LoggingFilter
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal

object ContactFrontendGlobal extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = AuditConnector
  override val loggingFilter = CFLoggingFilter
  override val frontendAuditFilter = ContactFrontendAuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode): Configuration = {
    AuthParamsConfigurationValidator.validate(config)
    super.onLoadConfig(config, path, classloader, mode)
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    views.html.error_template(pageTitle, heading, message)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object CFLoggingFilter extends LoggingFilter {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object ContactFrontendAuditFilter extends FrontendAuditFilter with RunMode with AppName {

  override lazy val maskedFormFields = Seq.empty

  override lazy val applicationPort = None

  override lazy val auditConnector = AuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}


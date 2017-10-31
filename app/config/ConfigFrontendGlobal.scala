
package config

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

object ContactFrontendGlobal
  extends DefaultFrontendGlobal
  with RunMode {

  override def auditConnector = Play.current.injector.instanceOf[AuditConnector]
  override def loggingFilter = CFLoggingFilter
  override def frontendAuditFilter = ContactFrontendAuditFilter
  override def frontendFilters = defaultFrontendFilters ++ Seq(CorsFilter)
  implicit lazy val config = Play.current.injector.instanceOf[AppConfig]

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

  override lazy val auditConnector = Play.current.injector.instanceOf[AuditConnector]

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}


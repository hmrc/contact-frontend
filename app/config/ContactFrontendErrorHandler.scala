package config

import javax.inject.Inject

import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

class ContactFrontendErrorHandler @Inject()(val messagesApi: MessagesApi, val configuration: Configuration)(implicit appConfig : AppConfig)
  extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]) =
    views.html.error_template(pageTitle, heading, message)
}

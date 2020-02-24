package config

import javax.inject.Inject

import play.api.Configuration
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

class ContactFrontendErrorHandler @Inject()(val configuration: Configuration,
                                            val messagesApi: MessagesApi)
                                           (implicit appConfig: AppConfig)
    extends FrontendErrorHandler {

  implicit val lang: Lang = Lang.defaultLang

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]) =
    views.html.error_template(pageTitle, heading, message)
}

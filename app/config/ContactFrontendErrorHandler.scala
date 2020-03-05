package config

import javax.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

class ContactFrontendErrorHandler @Inject()(val messagesApi: MessagesApi)
                                           (implicit appConfig: AppConfig)
    extends FrontendErrorHandler {

  implicit def lang(implicit request: Request[_]): Lang = request.lang(messagesApi)

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]) =
    views.html.error_template(pageTitle, heading, message)
}

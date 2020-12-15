/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package config

import javax.inject.Inject
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Request
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.ErrorPage

class ContactFrontendErrorHandler @Inject() (val messagesApi: MessagesApi, errorPage: ErrorPage)(implicit
  appConfig: AppConfig
) extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ) =
    errorPage(pageTitle, heading, message)
}

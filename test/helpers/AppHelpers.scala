/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package helpers

import play.api.Application
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.RequestHeader

trait AppHelpers {
  def getMessages(implicit app: Application, request: RequestHeader) = {
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
    messagesApi.preferred(request)
  }

  def getWelshMessages(implicit app: Application, request: RequestHeader) = {
    val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
    messagesApi.preferred(Seq(Lang("cy")))
  }
}

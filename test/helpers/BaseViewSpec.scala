/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers

import config.AppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.RequestHeader
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest

import scala.reflect.ClassTag

trait BaseViewSpec extends AnyWordSpec with Matchers with ApplicationSupport with JsoupHelpers {

  def instanceOf[A: ClassTag]: A = app.injector.instanceOf[A]

  private def getMessages()(using request: RequestHeader): Messages = {
    val messagesApi: MessagesApi = instanceOf[MessagesApi]
    messagesApi.preferred(request)
  }

  val getWelshMessages: Messages = {
    val messagesApi: MessagesApi = instanceOf[MessagesApi]
    messagesApi.preferred(Seq(Lang("cy")))
  }

  given fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken
  given Messages                   = getMessages()

  given AppConfig = instanceOf[AppConfig]

}

/*
 * Copyright 2023 HM Revenue & Customs
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

package a11y

import _root_.helpers.{ApplicationSupport, MessagesSupport}
import config.AppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.ErrorPage

class ErrorPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with AccessibilityMatchers {

  given fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken
  given Messages                   = getMessages()
  given AppConfig                  = app.injector.instanceOf[AppConfig]

  "the error template" should {
    val errorTemplate = app.injector.instanceOf[ErrorPage]
    val content       =
      errorTemplate(pageTitle = "This is the title", heading = "This is the heading", message = "This is the message.")

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

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
import model.AccessibilityForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.AccessibilityProblemPage

class AccessibilityProblemPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
      with AccessibilityMatchers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val accessibilityForm: Form[AccessibilityForm] = Form[AccessibilityForm](
    mapping(
      "problemDescription" -> text.verifying("accessibility.problem.error.required", msg => msg.nonEmpty),
      "name"               -> text.verifying("accessibility.name.error.required", msg => msg.nonEmpty),
      "email"              -> text.verifying("accessibility.email.error.invalid", msg => msg.nonEmpty),
      "isJavascript"       -> boolean,
      "referrer"           -> text,
      "csrfToken"          -> text,
      "service"            -> optional(text),
      "userAction"         -> optional(text)
    )(AccessibilityForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  val action: Call = Call(method = "POST", url = "/contact/the-submit-url")

  "the report an accessibility problem page" should {
    val accessibilityProblemPage = app.injector.instanceOf[AccessibilityProblemPage]
    val content                  = accessibilityProblemPage(accessibilityForm, action)

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

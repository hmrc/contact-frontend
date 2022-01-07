/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.ReportProblemConfirmationPage

class ReportProblemConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with AccessibilityMatchers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/submit")
  implicit lazy val messages: Messages         = getMessages(app, fakeRequest)
  implicit lazy val appConfig: AppConfig       = app.injector.instanceOf[AppConfig]

  "the Problem Reports standalone confirmation page" should {
    val confirmationPage = app.injector.instanceOf[ReportProblemConfirmationPage]
    val content          = confirmationPage()

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

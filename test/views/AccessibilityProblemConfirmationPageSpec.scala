/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import config.AppConfig
import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import views.html.AccessibilityProblemConfirmationPage

class AccessibilityProblemConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo")
  implicit lazy val messages: Messages         = getMessages(app, fakeRequest)
  implicit lazy val appConfig: AppConfig       = app.injector.instanceOf[AppConfig]

  "the report an accessibility problem confirmation page" should {
    val accessibilityProblemConfirmationPage = app.injector.instanceOf[AccessibilityProblemConfirmationPage]
    val content                              = accessibilityProblemConfirmationPage()

    "include the confirmation panel" in {
      val panels = content.select("h1.govuk-panel__title")
      panels            should have size 1
      panels.first.text should be("Your accessibility problem has been reported.")
    }

    "include the what happens next section" in {
      val headings = content.select("h2.govuk-heading-l")
      headings            should have size 1
      headings.first.text should be("What happens next")
    }

    "translate the title into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = accessibilityProblemConfirmationPage()

      val titles = welshContent.select("h1.govuk-panel__title")
      titles.first.text should be("Mae’ch problem wedi ei nodi")
    }
  }
}

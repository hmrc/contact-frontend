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

import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import config.AppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import views.html.FeedbackConfirmationPage

class FeedbackConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo")
  implicit lazy val messages: Messages         = getMessages(app, fakeRequest)
  implicit lazy val appConfig: AppConfig       = app.injector.instanceOf[AppConfig]

  "the feedback confirmation page" should {
    val feedbackConfirmationPage = app.injector.instanceOf[FeedbackConfirmationPage]
    val content                  = feedbackConfirmationPage()

    "include the confirmation panel" in {
      val panels = content.select("h1")
      panels            should have size 1
      panels.first.text should be("Your feedback has been received.")
    }

    "include a back link" in {
      val contentWithBackLink = feedbackConfirmationPage(backUrl = Some("/foo"))
      val backlinks           = contentWithBackLink.select("a.govuk-link")
      backlinks                     should have size 2
      backlinks.get(1).attr("href") should be("/foo")
    }

    "translate the title into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = feedbackConfirmationPage()

      val titles = welshContent.select("h1")
      titles.first.text should be("Mae eich adborth wedi dod i law.")
    }
  }
}

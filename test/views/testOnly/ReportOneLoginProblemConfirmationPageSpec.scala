/*
 * Copyright 2025 HM Revenue & Customs
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

package views.testOnly

import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import config.AppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import views.html.testOnly.ReportOneLoginProblemConfirmationPage

class ReportOneLoginProblemConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  given fakeRequest: RequestHeader = FakeRequest("GET", "/submit")
  given Messages                   = getMessages()
  given AppConfig                  = app.injector.instanceOf[AppConfig]

  "the Problem Reports standalone confirmation page" should {
    val confirmationPage = app.injector.instanceOf[ReportOneLoginProblemConfirmationPage]
    val content          = confirmationPage()

    "include the H1 element with page title" in {
      val heading1 = content.select("h1")
      heading1            should have size 1
      heading1.first.text should be("We have received your technical problem report")
    }

    "include the H2 element for what happens next" in {
      val heading2 = content.select("h2")
      heading2.first.text should be("What happens next")
    }

    "include a paragraph body element with confirmation submission" in {
      val paragraphs = content.select("p.govuk-body")
      paragraphs.get(0).text should be("We will send a confirmation email to the email address you provided.")
    }

    "include a paragraph body element with what will happen next" in {
      val paragraphs = content.select("p.govuk-body")
      paragraphs.get(1).text() should be("Someone will email you back within 2 working days with the next steps.")
    }

    "translate the title into Welsh if requested" in {
      given Messages   = getWelshMessages()
      val welshContent = confirmationPage()

      val titles = welshContent.select("h1")
      titles.first.text should be("Mae’ch adroddiad ynghylch problem dechnegol wedi dod i law")
    }
  }
}

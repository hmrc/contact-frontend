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

package views

import helpers.BaseViewSpec
import play.api.i18n.Messages
import views.html.FeedbackConfirmationPage

class FeedbackConfirmationPageSpec extends BaseViewSpec {

  "the feedback confirmation page" should {
    val feedbackConfirmationPage = instanceOf[FeedbackConfirmationPage]
    val content                  = feedbackConfirmationPage()

    "include the confirmation panel" in {
      val panels = content.select("h1")
      panels            should have size 1
      panels.first.text should be("We have received your feedback")
    }

    "include a back link" in {
      val contentWithBackLink = feedbackConfirmationPage(backUrl = Some("/foo"))
      val backlinks           = contentWithBackLink.select("a.govuk-back-link")
      backlinks                     should have size 1
      backlinks.get(0).attr("href") should be("/foo")
    }

    "translate the title into Welsh if requested" in {
      given Messages   = getWelshMessages
      val welshContent = feedbackConfirmationPage()

      val titles = welshContent.select("h1")
      titles.first.text should be("Mae’ch adborth wedi dod i law")
    }
  }
}

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
import play.api.test.Helpers.*
import views.html.InternalErrorPage

class InternalErrorPageSpec extends BaseViewSpec {

  "the error template" should {
    val errorTemplate = instanceOf[InternalErrorPage]
    val content       =
      errorTemplate()

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "not include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 0
    }

    "display the correct browser title" in {
      content.select("title").first().text shouldBe "Sorry, there is a problem with the service – Contact HMRC – GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Sorry, there is a problem with the service"
    }

    "return the deskpro specific content" in {
      contentAsString(content) should include(
        "Your message has not been sent."
      )
    }

    "return the generic content" in {
      contentAsString(content) should include(
        "Try again later."
      )
    }
  }
}

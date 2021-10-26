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

package acceptance.specs

import acceptance.pages.{SurveyPage, SurveyThanksPage}
import acceptance.specs.tags.UiTests
import acceptance.support.CustomMatchers.containInOrder

class SurveySpec extends BaseSpec {

  info("UI tests for /contact/survey")

  Feature("Successfully submit a survey form") {

    Scenario("I am able to successfully submit a survey form", UiTests) {

      Given("I am on the survey page")
      go to SurveyPage
      pageTitle shouldBe SurveyPage.pageTitle

      When("I complete all the fields")
      SurveyPage.completeReportForm()

      And("I submit the form")
      SurveyPage.submitForm

      Then("I see the submission confirmation page")
      eventually {
        pageTitle shouldBe SurveyThanksPage.pageTitle
      }

      SurveyThanksPage.heading shouldBe SurveyThanksPage.expectedHeading
    }
  }

  Feature("Validation fails when submitting an incomplete survey form") {

    Scenario("I do not complete all the fields", UiTests) {

      Given("I am on the survey page")
      go to SurveyPage
      pageTitle shouldBe SurveyPage.pageTitle

      When("When I do not complete all the fields")

      And("I submit the form")
      SurveyPage.submitForm

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe SurveyPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Tell us how satisfied you are with the answer we gave you",
        "Tell us how satisfied you are with the speed of our reply"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Language switching") {

    Scenario("I switch my language to Welsh", UiTests) {

      Given("I am on the survey page")
      go to SurveyPage
      pageTitle shouldBe SurveyPage.pageTitle

      When("When I use the language switch toggle")
      click on partialLinkText("Cymraeg")

      Then("I see the survey page in Welsh")
      eventually {
        pageTitle shouldBe SurveyPage.welshPageTitle
      }
    }
  }
}

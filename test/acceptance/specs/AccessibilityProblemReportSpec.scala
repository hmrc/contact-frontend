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

import acceptance.pages.{AccessibilityProblemReportPage, AccessibilityProblemReportThanksPage}
import acceptance.specs.tags.UiTests
import acceptance.support.CustomMatchers.containInOrder

class AccessibilityProblemReportSpec extends BaseSpec {

  info("UI tests for /contact/accessibility")

  Feature("Successfully submit a report accessibility problem form") {

    Scenario("I am able to successfully submit a report accessibility problem form", UiTests) {

      Given("I am on the report accessibility problem page")
      go to AccessibilityProblemReportPage
      pageTitle shouldBe AccessibilityProblemReportPage.pageTitle

      When("I complete all the fields")
      AccessibilityProblemReportPage.completeReportForm()

      And("I submit the form")
      AccessibilityProblemReportPage.submitForm

      Then("I see the submission confirmation page")
      eventually {
        pageTitle shouldBe AccessibilityProblemReportThanksPage.pageTitle
      }

      AccessibilityProblemReportThanksPage.heading    shouldBe AccessibilityProblemReportThanksPage.expectedHeading
      AccessibilityProblemReportThanksPage.subHeading shouldBe AccessibilityProblemReportThanksPage.expectedSubheading
    }
  }

  Feature("Validation fails when submitting an incomplete report accessibility problem form") {

    Scenario("I do not complete all the fields", UiTests) {

      Given("I am on the report accessibility problem page")
      go to AccessibilityProblemReportPage
      pageTitle shouldBe AccessibilityProblemReportPage.pageTitle

      When("When I do not complete all the fields ")

      And("I submit the form")
      AccessibilityProblemReportPage.submitForm

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe AccessibilityProblemReportPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Enter details of the accessibility problem",
        "Enter your full name",
        "Enter your email address"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Validation fails when submitting an invalid email address") {

    Scenario("I provide an invalid email address", UiTests) {

      Given("I am on the report accessibility problem page")
      go to AccessibilityProblemReportPage
      pageTitle shouldBe AccessibilityProblemReportPage.pageTitle

      When("When I provide an invalid email address ")
      AccessibilityProblemReportPage.completeReportForm(email = "firstname.lastname")

      And("I submit the form")
      AccessibilityProblemReportPage.submitForm

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe AccessibilityProblemReportPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Enter an email address in the correct format, like name@example.com"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Validation fails when submitting too long comment") {

    Scenario("I put too many characters in the text field", UiTests) {

      Given("I am on the report accessibility problem page")
      go to AccessibilityProblemReportPage
      pageTitle shouldBe AccessibilityProblemReportPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      AccessibilityProblemReportPage.completeReportForm(commentsLength = 2001)

      And("I submit the form")
      AccessibilityProblemReportPage.submitForm

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe AccessibilityProblemReportPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Problem description must be 2000 characters or fewer"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Client side validation warns for too long comment") {

    Scenario("I put too many characters in the text field", UiTests) {

      Given("I am on the report accessibility problem page")
      go to AccessibilityProblemReportPage
      pageTitle shouldBe AccessibilityProblemReportPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      AccessibilityProblemReportPage.completeReportForm(commentsLength = 2001)

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe AccessibilityProblemReportPage.pageTitle
      }

      val orderedErrorMessages = List(
        "You have 1 character too many"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Language switching") {

    Scenario("I switch my language to Welsh", UiTests) {

      Given("I am on the report accessibility problem page")
      go to AccessibilityProblemReportPage
      pageTitle shouldBe AccessibilityProblemReportPage.pageTitle

      When("When I use the language switch toggle")
      click on partialLinkText("Cymraeg")

      Then("I see the help and contact page in Welsh")
      eventually {
        pageTitle shouldBe AccessibilityProblemReportPage.welshPageTitle
      }
    }
  }
}

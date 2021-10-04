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

import acceptance.pages.{BetaFeedbackPage, BetaFeedbackThanksPage}
import acceptance.specs.tags.UiTests
import acceptance.support.CustomMatchers.containInOrder

class BetaFeedbackSpec extends BaseSpec {

  info("UI tests for /contact/beta-feedback")

  Feature("Successfully submit a beta feedback form") {

    Scenario("I am able to successfully submit a feedback form", UiTests) {

      Given("I am on the send your feedback page")
      go to BetaFeedbackPage
      pageTitle shouldBe BetaFeedbackPage.pageTitle

      When("I complete all the fields")
      BetaFeedbackPage.completeReportForm()

      And("I submit the form")
      BetaFeedbackPage.submitReportForm

      Then("I see the submission confirmation page")
      eventually {
        pageTitle shouldBe BetaFeedbackThanksPage.pageTitle
      }

      BetaFeedbackThanksPage.heading shouldBe BetaFeedbackThanksPage.expectedHeading
    }
  }

  Feature("Validation fails when submitting an incomplete beta feedback form") {

    Scenario("I do not complete all the fields", UiTests) {

      Given("I am on the send your feedback page")
      go to BetaFeedbackPage
      pageTitle shouldBe BetaFeedbackPage.pageTitle

      When("When I do not complete all the fields ")

      And("I submit the form")
      BetaFeedbackPage.submitReportForm

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe BetaFeedbackPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Tell us what you think of the service",
        "Enter your full name",
        "Enter an email address in the correct format, like name@example.com",
        "Enter your comments"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Validation fails when submitting an invalid email address") {

    Scenario("I provide an invalid email address", UiTests) {

      Given("I am on the send your feedback page")
      go to BetaFeedbackPage
      pageTitle shouldBe BetaFeedbackPage.pageTitle

      When("When I provide an invalid email address ")
      BetaFeedbackPage.completeReportForm(email = "firstname.lastname")

      And("I submit the form")
      BetaFeedbackPage.submitReportForm

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe BetaFeedbackPage.errorPageTitle
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

      Given("I am on the send your feedback page")
      go to BetaFeedbackPage
      pageTitle shouldBe BetaFeedbackPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      BetaFeedbackPage.completeReportForm(commentsLength = 2001)

      And("I submit the form")
      BetaFeedbackPage.submitReportForm

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe BetaFeedbackPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "The comment cannot be longer than 1000 characters"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Client side validation warns for too long comment") {

    Scenario("I put too many characters in the text field", UiTests) {

      Given("I am on the send your feedback page")
      go to BetaFeedbackPage
      pageTitle shouldBe BetaFeedbackPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      BetaFeedbackPage.completeReportForm(commentsLength = 2001)

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe BetaFeedbackPage.pageTitle
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

      Given("I am on the send your feedback page")
      go to BetaFeedbackPage
      pageTitle shouldBe BetaFeedbackPage.pageTitle

      When("When I use the language switch toggle")
      click on partialLinkText("Cymraeg")

      Then("I see the beta-feedback page in Welsh")
      eventually {
        pageTitle shouldBe BetaFeedbackPage.welshPageTitle
      }
    }
  }
}

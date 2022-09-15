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

package acceptance.specs

import acceptance.pages.{ContactHmrcPage, ContactHmrcThanksPage}
import acceptance.specs.tags.UiTests
import acceptance.support.CustomMatchers.containInOrder

class ContactHmrcSpec extends BaseSpec {

  info("UI tests for /contact/contact-hmrc")

  Feature("Successfully submit a help and contact form") {

    Scenario("I am able to successfully submit a help and contact form", UiTests) {

      Given("I am on the help and contact page")
      go to ContactHmrcPage
      pageTitle shouldBe ContactHmrcPage.pageTitle

      When("I complete all the fields")
      ContactHmrcPage.completeReportForm()

      And("I submit the form")
      ContactHmrcPage.submitForm()

      Then("I see the submission confirmation page")
      eventually {
        pageTitle shouldBe ContactHmrcThanksPage.pageTitle
      }

      ContactHmrcThanksPage.heading    shouldBe ContactHmrcThanksPage.expectedHeading
      ContactHmrcThanksPage.subHeading shouldBe ContactHmrcThanksPage.expectedSubheading
    }
  }

  Feature("Validation fails when submitting an incomplete help and contact form") {

    Scenario("I do not complete all the fields", UiTests) {

      Given("I am on the help and contact page")
      go to ContactHmrcPage
      pageTitle shouldBe ContactHmrcPage.pageTitle

      When("When I do not complete all the fields ")

      And("I submit the form")
      ContactHmrcPage.submitForm()

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe ContactHmrcPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Enter your email address",
        "Enter your full name",
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

      Given("I am on the help and contact page")
      go to ContactHmrcPage
      pageTitle shouldBe ContactHmrcPage.pageTitle

      When("When I provide an invalid email address ")
      ContactHmrcPage.completeReportForm(email = "firstname.lastname")

      And("I submit the form")
      ContactHmrcPage.submitForm()

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe ContactHmrcPage.errorPageTitle
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

      Given("I am on the help and contact page")
      go to ContactHmrcPage
      pageTitle shouldBe ContactHmrcPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      ContactHmrcPage.completeReportForm(commentsLength = 2001)

      And("I submit the form")
      ContactHmrcPage.submitForm()

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe ContactHmrcPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Comment must be 2000 characters or fewer"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Client side validation warns for too long comment") {

    Scenario("I put too many characters in the text field", UiTests) {

      Given("I am on the help and contact page")
      go to ContactHmrcPage
      pageTitle shouldBe ContactHmrcPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      ContactHmrcPage.completeReportForm(commentsLength = 2001)

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe ContactHmrcPage.pageTitle
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

      Given("I am on the help and contact page")
      go to ContactHmrcPage
      pageTitle shouldBe ContactHmrcPage.pageTitle

      When("When I use the language switch toggle")
      click on partialLinkText("Cymraeg")

      Then("I see the help and contact page in Welsh")
      eventually {
        pageTitle shouldBe ContactHmrcPage.welshPageTitle
      }
    }
  }
}

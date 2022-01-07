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

import acceptance.pages.{ReportProblemPage, ReportProblemThanksPage}
import acceptance.specs.tags.UiTests
import acceptance.support.CustomMatchers.containInOrder

class ReportProblemSpec extends BaseSpec {

  info("UI tests for /contact/problem_reports_nonjs")

  Feature("Successfully submit a problem report form") {

    Scenario("I am able to successfully submit a problem report form", UiTests) {

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("I submit the report")
      ReportProblemPage.completeReportForm()
      ReportProblemPage.submitForm()

      Then("I see the thank you page")
      eventually {
        pageTitle shouldBe ReportProblemThanksPage.pageTitle
      }

      ReportProblemThanksPage.heading    shouldBe ReportProblemThanksPage.expectedHeading
      ReportProblemThanksPage.subHeading shouldBe ReportProblemThanksPage.expectedSubHeading
    }
  }

  Feature("Validation fails when submitting a report technical problem form with invalid name") {

    Scenario("I submit a problem report form with invalid characters in name", UiTests) {

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("I enter an invalid character in the name field")
      ReportProblemPage.completeReportForm(name = "Firstname & Lastname")

      And("I submit the report")
      ReportProblemPage.submitForm()

      Then("I see an error message with the correct format to follow")
      eventually {
        pageTitle shouldBe ReportProblemPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Full name must only include letters a to z, hyphens, full stops, commas, apostrophes and spaces"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Validation fails when submitting an incomplete report technical problem form") {

    Scenario("I do not complete all the fields", UiTests) {

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("When I do not complete all the fields ")

      And("I submit the form")
      ReportProblemPage.submitForm

      Then("Then I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe ReportProblemPage.errorPageTitle
      }

      val orderedErrorMessages = List(
        "Enter your full name",
        "Enter your email address",
        "Enter details of what you were doing",
        "Enter details of what went wrong"
      )

      eventually {
        val body = tagName("body").element
        body.text should containInOrder(orderedErrorMessages)
      }
    }
  }

  Feature("Validation fails when submitting an invalid email address") {

    Scenario("I provide an invalid email address", UiTests) {

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("When I provide an invalid email address ")
      ReportProblemPage.completeReportForm(email = "firstname.lastname")

      And("I submit the form")
      ReportProblemPage.submitForm

      Then("I see an error message citing the required fields")
      eventually {
        pageTitle shouldBe ReportProblemPage.errorPageTitle
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

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      ReportProblemPage.completeReportForm(actionLength = 1001)

      And("I submit the form")
      ReportProblemPage.submitForm

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe ReportProblemPage.errorPageTitle
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

  Feature("Client side validation warns for too long action comment") {

    Scenario("I put too many characters in the action field", UiTests) {

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("When I write more than the allocated characters in a text field")
      ReportProblemPage.completeReportForm(actionLength = 1001)

      Then("I see an error message telling me that I have exceeded the character limit")
      eventually {
        pageTitle shouldBe ReportProblemPage.pageTitle
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

      Given("I am on the report a technical problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("When I use the language switch toggle")
      click on partialLinkText("Cymraeg")

      Then("I see the help and contact page in Welsh")
      eventually {
        pageTitle shouldBe ReportProblemPage.welshPageTitle
      }
    }
  }

}

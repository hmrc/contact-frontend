package features

import support.StubbedFeatureSpec
import support.page.{PleaseTryAgainPage, ThankYouPage, UnauthenticatedFeedbackPage}
import support.steps.Env

class GetHelpWithThisPageFeature_NoJavascript extends StubbedFeatureSpec {

  val Name = "Grumpy Bear"
  val Email = "grumpy@carebears.com"
  val WhatWhereYouDoing = "Something"
  val WhatDoYouNeedHelpWith = "Nothing"

  feature("Get help with this page form") {

    info("In order to get help with a specific page")
    info("As a tax payer with Javascript disabled")
    info("I want to ask for help to HMRC")

    scenario("I don't see the open help form link without Javascript") {
      Given("JavaScript is disabled")
      Env.useNoJsDriver()

      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Then("I don't see:")
      i_dont_see("Get help with this page.")
    }

    scenario("Successful form submission without Javascript") {
      Given("JavaScript is disabled")
      Env.useNoJsDriver()

      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      When("I fill the Get Help with this page' form correctly")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.fillProblemReport(Name, Email, WhatWhereYouDoing, WhatDoYouNeedHelpWith)

      And("I send the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.submitProblemReport()

      Then("I am on the success page")
      on(ThankYouPage)

      Then("I see:")
      i_see(
        "Thank you",
        "Someone will get back to you within 2 working days."
      )
    }

    scenario("Only these characters are allowed for the name: letters (lower and upper case), space, comma, period, braces and hyphen") {
      Given("JavaScript is disabled")
      Env.useNoJsDriver()

      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      When("I fill the Get Help with this page' form correctly")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.fillProblemReport("Hello <&^$%£", Email, WhatWhereYouDoing, WhatDoYouNeedHelpWith)

      And("I send the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.submitProblemReport(javascriptEnabled = false)

      Then("I am on the 'Please try again' page")
      on(PleaseTryAgainPage)

      And("I see an error message")
      i_see("There was a problem sending your query",
            "Please try again later.")
    }

    scenario("All fields are mandatory") {
      Given("JavaScript is disabled")
      Env.useNoJsDriver()

      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.clickSubmitButton()

      Then("I see an error for invalid name")
      i_see("There was a problem sending your query",
        "Please try again later.")
    }
  }

}

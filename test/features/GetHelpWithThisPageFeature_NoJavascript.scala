package features

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import support.behaviour.NavigationSugar
import support.page.{PleaseTryAgainPage, ThankYouPage, UnauthenticatedFeedbackPage}
import support.steps.ObservationSteps
import support.stubs.NoJsFeature

class GetHelpWithThisPageFeature_NoJavascript extends NoJsFeature with ScalaFutures with IntegrationPatience with NavigationSugar with ObservationSteps {


  Feature("Get help with this page form") {

    info("In order to get help with a specific page")
    info("As a tax payer with Javascript disabled")
    info("I want to ask for help to HMRC")



    Scenario("Successful form submission without JavaScript") {
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
        "Your message has been sent, and the team will get back to you within 2 working days."
      )
    }

    Scenario("Only these characters are allowed for the name: letters (lower and upper case), space, comma, period, braces and hyphen") {
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
            "Please try again later or email hmrcsupport@tax.service.gov.uk if you need technical help with this website.")
    }

    Scenario("All fields are mandatory") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.clickSubmitButton()

      Then("I see an error for invalid name")
      i_see("There was a problem sending your query",
        "Please try again later or email hmrcsupport@tax.service.gov.uk if you need technical help with this website.")
    }
  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val WhatWhereYouDoing = "Something"
  private val WhatDoYouNeedHelpWith = "Nothing"

}
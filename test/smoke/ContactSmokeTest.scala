package smoke

import org.scalatest.AcceptanceSpec
import support.StubbedFeatureSpec
import support.behaviour.NavigationSugar
import support.page.{ContactHmrcPage, UnauthenticatedFeedbackPage}
import support.steps.{DeskproSteps, ObservationSteps, SmokeSteps}
import uk.gov.hmrc.integration.util.RandomUtils

//class ContactSmokeTest extends AcceptanceSpec with NavigationSugar with ObservationSteps with SmokeSteps with DeskproSteps {
class ContactSmokeTest extends StubbedFeatureSpec with SmokeSteps with DeskproSteps {


  feature("Contact HMRC") {

    info("In order to make my views known")
    info("As a Tax Payer")
    info("I want to contact HMRC")

//    MoveToAcceptanceTest: End to end test
    scenario("Problem report - Get help with this page") {
      Given("Tax payer Bill goes to the Feedback page")

      goOn(UnauthenticatedFeedbackPage)

      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      When("He fills out and sends the problem report")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.sendProblemReport(Name, Email, WhatIWasDoing, WhatWentWrong)

      Then("He sees a success message")
      i_see("Thank you",
            "Someone will get back to you within 2 working days.")
    }

//    MoveToAcceptanceTest: End to end test
    scenario("Feedback submission") {
      Given("Tax payer Bill goes to the Feedback page")
      goOn(UnauthenticatedFeedbackPage)

      When("He fills out and sends the contact form")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(5, Name, Email, Comment)
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("He sees a success message")
      i_see("Thank you",
            "Your feedback has been received.")
    }


//    MoveToAcceptanceTest: End to end test
    scenario("Contact HMRC") {
      Given("Tax payer Bill goes to the Contact HMRC page")
      i_sign_in()
      goOn(ContactHmrcPage)

      When("He fills out and sends the contact form")
      ContactHmrcPage.sendContactForm(Name, Email, Comment)

      Then("He sees a success message")
      i_see("Thank you",
        "Someone will get back to you within 2 working days.")
    }
  }
  private val randomString = RandomUtils.randString(5)
  private val Name = s"Grumpy Bear ${randomString}"
  private val Email = s"grumpy_${randomString}@carebears.com"
  private val WhatIWasDoing = s"I was trying to do something ${randomString}"
  private val WhatWentWrong = s"I could not figure out how ${randomString}"
  private val Comment = s"I am writing a comment ${randomString}"

}

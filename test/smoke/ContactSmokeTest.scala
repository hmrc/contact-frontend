package smoke

import org.scalatest.AcceptanceSpec
import support.behaviour.NavigationSugar
import support.page.{UnauthenticatedFeedbackPage, ContactHmrcPage}
import support.steps.{DeskproSteps, NavigationSteps, ObservationSteps, SmokeSteps}

class ContactSmokeTest extends AcceptanceSpec with NavigationSugar with ObservationSteps with SmokeSteps with DeskproSteps {

  Feature("Contact HMRC") {

    info("In order to make my views known")
    info("As a Tax Payer")
    info("I want to contact HMRC")


    Scenario("Problem report - Get help with this page") {
      Given("Tax payer Bill goes to the Feedback page")
      goOn(UnauthenticatedFeedbackPage)

      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      When("He fills out and sends the problem report")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.sendProblemReport(Name, Email, WhatIWasDoing, WhatWentWrong)

      Then("He sees a success message")
      i_see("Thank you",
            "Someone will get back to you within 2 working days.")

      And("Support agent Ann receives a ticket via Deskpro")
      ticket_in_deskpro_exists(UnauthenticatedFeedbackPage.getHelpWithThisPage.ticketId.value, Name, Email, Seq(WhatIWasDoing, WhatWentWrong))
    }


    Scenario("Feedback submission") {
      Given("Tax payer Bill goes to the Feedback page")
      goOn(UnauthenticatedFeedbackPage)

      When("He fills out and sends the contact form")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(5, Name, Email, Comment)
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("He sees a success message")
      i_see("Thank you",
            "Your feedback has been received.")

      And("Support agent Ann receives a ticket via Deskpro")
      ticket_in_deskpro_exists(UnauthenticatedFeedbackPage.getHelpWithThisPage.ticketId.value, Name, Email, Seq(Comment))
    }


    Scenario("Contact HMRC") {
      Given("Tax payer Bill goes to the Contact HMRC page")
      i_sign_in()
      goOn(ContactHmrcPage)

      When("He fills out and sends the contact form")
      ContactHmrcPage.sendContactForm(Name, Email, Comment)

      Then("He sees a success message")
      i_see("Thank you",
        "Someone will get back to you within 2 working days.")

      And("Support agent Ann receives a ticket via Deskpro")
      ticket_in_deskpro_exists(ContactHmrcPage.getHelpWithThisPage.ticketId.value, Name, Email, Seq(Comment))
    }
  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val WhatIWasDoing = "I was trying to do something"
  private val WhatWentWrong = "I could not figure out how"
  private val Comment = "I am writing a comment"

}

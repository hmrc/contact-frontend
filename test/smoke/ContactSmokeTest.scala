package smoke

import org.scalatest.AcceptanceSpec
import support.page.{ContactHmrcPage, FeedbackPage}
import support.steps.{DeskproSteps, NavigationSteps, ObservationSteps, SmokeSteps}

class ContactSmokeTest extends AcceptanceSpec with NavigationSteps with ObservationSteps with SmokeSteps with DeskproSteps {

  Feature("Contact HMRC") {

    info("In order to make my views known")
    info("As a Tax Payer")
    info("I want to contact HMRC")


    Scenario("Problem report - Get help with this page") {
      Given("Tax payer Bill goes to the Feedback page")
      val page = new FeedbackPage
      go to page

      When("He fills out and sends the problem report")
      page.GetHelpWithThisPage.sendProblemReport(Name, Email, WhatIWasDoing, WhatWentWrong)

      Then("He sees a success message")
      i_see("Thank you",
            "Your message has been sent, and the team will get back to you within 2 working days.")

      And("Support agent Ann receives a ticket via Deskpro")
      ticket_in_deskpro_exists(page.GetHelpWithThisPage.ticketId.value, Name, Email, Seq(WhatIWasDoing, WhatWentWrong))
    }


    Scenario("Contact HMRC") {
      Given("Tax payer Bill goes to the Contact HMRC page")
      val contactHmrcPage = new ContactHmrcPage
      go to contactHmrcPage

      When("He fills out and sends the contact form")
      contactHmrcPage.sendContactForm(Name, Email, Comment)

      Then("He sees a success message")
      i_see("Thank you",
            "Your message has been sent, and the team will get back to you within 2 working days.")

      And("Support agent Ann receives a ticket via Deskpro")
      ticket_in_deskpro_exists(contactHmrcPage.GetHelpWithThisPage.ticketId.value, Name, Email, Seq(Comment))
    }


    Scenario("Feedback submission") {
      Given("Tax payer Bill goes to the Feedback page")
      val feedbackPage = new FeedbackPage
      go to feedbackPage

      When("He fills out and sends the contact form")
      feedbackPage.fillOutFeedbackForm(5, Name, Email, Comment)
      feedbackPage.submitFeedbackForm()

      Then("He sees a success message")
      i_see("Thank you",
            "Your feedback has been received.")

      And("Support agent Ann receives a ticket via Deskpro")
      ticket_in_deskpro_exists(feedbackPage.GetHelpWithThisPage.ticketId.value, Name, Email, Seq(Comment))
    }

  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val WhatIWasDoing = "I was trying to do something"
  private val WhatWentWrong = "I could not figure out how"
  private val Comment = "I am writing a comment"

}

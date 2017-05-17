package smoke

import org.scalatest.AcceptanceSpec
import support.behaviour.NavigationSugar
import support.page.{ContactHmrcPage, UnauthenticatedFeedbackPage}
import support.steps.{DeskproSteps, ObservationSteps, SmokeSteps}
import uk.gov.hmrc.integration.GovernmentGatewayUsers

class ContactSmokeTest extends AcceptanceSpec with NavigationSugar with ObservationSteps with SmokeSteps with DeskproSteps {

  Feature("Contact HMRC") {

    info("In order to make my views known")
    info("As a Tax Payer")
    info("I want to contact HMRC")

    Scenario("Problem report - Get help with this page") {
      Given("Tax payer Bill goes to the Feedback page")
      goOn(UnauthenticatedFeedbackPage)

      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport()

      When("He fills out and sends the problem report")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.sendProblemReport(Name, Email, WhatIWasDoing, WhatWentWrong)

      Then("He sees a success message")
      i_see("Thank you",
            "Someone will get back to you within 2 working days.")

      val ticketNumber: String = UnauthenticatedFeedbackPage.getHelpWithThisPage.ticketId.value

      And(s"Support agent Ann receives a ticket [$ticketNumber] via Deskpro")
      ticket_in_deskpro_exists(ticketNumber, Name, Email, Seq(WhatIWasDoing, WhatWentWrong))
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
      Given(s"Tax payer Bill goes to the Contact HMRC page and signs in as [${GovernmentGatewayUsers.UserWithNoSARegime.username}]")
      waitForPageToLoad()

      iSignIn(GovernmentGatewayUsers.UserWithNoSARegime)

      /**** Beware of the Dragons ****/
      waitForPageToLoad() // <--- this makes it work :-/
      Thread.sleep(3000)  // <---- as long as this is here too... :-(
      // NOTE: Technically 2500ms is enough but I didn't want it to be any more fragile than it is already.
      /**** Beware of the Dragons ****/

      goOn(ContactHmrcPage)

      When("He fills out and sends the contact form")
      ContactHmrcPage.sendContactForm(Name, Email, Comment)

      Then("He sees a success message")
      i_see("Thank you",
        "Someone will get back to you within 2 working days.")

      /**** Beware of the Dragons ****/
      Thread.sleep(3000)
      /**** Beware of the Dragons ****/

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

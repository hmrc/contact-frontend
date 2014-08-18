package features

import org.skyscreamer.jsonassert.JSONCompareMode._
import support.page.ContactHmrcPage
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs.{Login, StubbedFeature}

class ContactHmrcFeature extends StubbedFeature with NavigationSteps with ApiSteps with ObservationSteps {

  Feature("Contact HMRC") {

    info("In order to make my views known")
    info("As a Tax Payer")
    info("I want to contact HMRC")


    Background {
      Given("I am logged in")

      And("I go to the 'Contact HMRC' page")
      go to new ContactHmrcPage
      i_am_on_the_page("Contact HMRC")
    }

    Scenario("Contact form sent successfully") {
      When("I fill the contact form correctly")
      val page = new ContactHmrcPage
      page.sendContactForm(Name, Email, Comment)

      And("I send the contact form")
      page.submitContactForm()

      Then("I see:")
      i_see("Thank you",
        "Your message has been sent, and the team will get back to you within 2 working days.")

      And("the Deskpro endpoint '/deskpro/ticket' has received the following POST request:")
      verify_post(to = "/deskpro/ticket", body =
        s"""
          |{
          |   "name":"$Name",
          |   "email":"$Email",
          |   "subject":"Contact form submission",
          |   "message":"$Comment",
          |   "referrer":"n/a",
          |   "javascriptEnabled":"Y",
          |   "authId":"/auth/oid/1234567890",
          |   "areaOfTax":"biztax",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }

//    Scenario("All fields are mandatory") {
//      When("I fill the form with empty values")
//      And("I try to send the contact form")
//      Then("I am on the 'Contact' page")
//      And("I see:")
//      //    i_see(
//      //      "Please provide your name",
//      //      "Enter a valid email address",
//      //      "Enter your comments")
//      And("the Deskpro enpoint '/deskpro/ticket' has not been hit")
//      pending
//    }
//
//
//    Scenario("Fields have a size limit") {
//      Given("the 'name' cannot be longer than 70 characters")
//      And("the 'email' cannot be longer than 255 characters")
//      And("the 'comment' cannot be longer than 2000 characters")
//      When("I fill the contact form with values that are too long")
//      Then("I am on the 'Contact' page")
//      And("I see:")
//      //    i_see(
//      //      "Your name cannot be longer than 70 characters",
//      //      "The email cannot be longer than 255 characters",
//      //      "The comment cannot be longer than 2000 characters")
//      pending
//    }
//
//
//    Scenario("Invalid email address") {
//      When("I fill the form with an invalid email address")
//      And("I try to send the contact form")
//      Then("I am on the 'Contact' page")
//      And("I see:")
//      //    i_see(
//      //      "Enter a valid email address")
//      pending
//    }
//
//
//    Scenario("Deskpro times out") {
//      Given("the call to Deskpro endpoint '/deskpro/ticket' will take XXX seconds")
//      When("I fill the contact form correctly")
//      And("I try to send the contact form")
//      Then("I am on the 'Contact' page")
//      And("I see:")
//      //      | .... |
//      pending
//    }
//
//
//    val statuses = Seq("404", "500")
//
//    statuses.foreach { status =>
//      Scenario(s"Deskpro fails with $status") {
//        Given(s"the call to Deskpro endpoint '/deskpro/ticket' will fail with status $status")
//        When("I fill the contact form correctly")
//        And("I try to send the contact form")
//        Then("I am on the 'Contact' page")
//        And("I see:")
//
//        pending
//      }
//    }
  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"
}
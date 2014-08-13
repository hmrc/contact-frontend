package features

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import support.page.{UnauthenticatedFeedbackPage, FeedbackPage}
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs.StubbedFeature

class FeedbackFeature extends StubbedFeature with NavigationSteps with ApiSteps with ObservationSteps {


  Feature("Feedback about the beta") {

    info("In order to make my views known about the beta")
    info("As a Tax Payer")
    info("I want to leave my feedback")


    Background {
      Given("I go to the 'Feedback' page")
      go to new UnauthenticatedFeedbackPage
      i_am_on_the_page("Send your feedback")
    }


    Scenario("Submit feedback successfully") {
      When("I fill the feedback form correctly")
      val page = new UnauthenticatedFeedbackPage
      page.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I send the feedback form")
      page.submitFeedbackForm()

      Then("I am on the 'Your feedback' page")
      i_am_on_the_page("Your feedback")

      Then("I see:")
      i_see(
        "Thank you",
        "Your feedback has been received."
      )

      And("the Deskpro endpoint '/deskpro/feedback' has received the following POST request:")
      verify_post(to = "/deskpro/feedback", body =
        s"""
          |{
          |   "name":"$Name",
          |   "email":"$Email",
          |   "subject":"Beta feedback submission",
          |   "rating":"1",
          |   "message":"$Comment",
          |   "referrer":"n/a",
          |   "javascriptEnabled":"Y",
          |   "authId":"n/a",
          |   "areaOfTax":"n/a",
          |   "sessionId":"n/a",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }

    /*Scenario("Feedback form sent successfully when signed in") {
      pending
    }



    Scenario("All fields are mandatory") {
      When("I fill the form with empty values")
      And("I try to send the feedback form")
      Then("I am on the 'Feedback' page")
      And("I see:")
//      i_see(
//        "Tell us what you think of the service.",
//        "Please provide your name.",
//        "Enter a valid email address.",
//        "Enter your comments."
//      )
      And("the Deskpro enpoint '/deskpro/ticket' has not been hit")

      pending
    }


    Scenario("Fields have a size limit") {
      Given("the 'name' cannot be longer than 70 characters")
      And("the 'email' cannot be longer than 255 characters")
      And("the 'comment' cannot be longer than 2000 characters")
      When("I fill the form with values that are too long")
      And("I try to send the feedback form")
      Then("I am on the 'Feedback' page")
      And("I see:")
//      i_see(
//        "The email cannot be longer than 255 characters ",
//        "Your name cannot be longer than 70 characters ",
//        "The comment cannot be longer than 2000 characters "
//      )

      pending
    }


    Scenario("Invalid email address") {
      When("I fill the form with an invalid email address")
      And("I try to send the feedback form")
      Then("I am on the 'Feedback' page")
      And("I see:")
//      i_see("Enter a valid email address ")

      pending
    }


    Scenario("Deskpro times out") {
      Given("the call to Deskpro endpoint '/deskpro/ticket' will take 10 seconds")
      When("I fill the feedback form correctly")
      And("I try to send the feedback form")
      Then("I am on the 'Feedback' page")
      And("I see:")

      pending
    }



    val statuses = Seq("404", "500")

    statuses foreach { status =>
      Scenario(s"Deskpro fails with status $status") {
        Given(s"the call to Deskpro endpoint '/deskpro/ticket' will fail with status $status")
        When("I fill the contact form correctly")
        And("I try to send the feedback form")
        Then("I am on the 'Feedback' page")
        And("I see:")

        pending
      }
    }
    */
  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"
}
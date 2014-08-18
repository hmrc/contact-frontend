package features

import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import support.page.{ExternalPage, UnauthenticatedFeedbackPage}
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs.{Stubs, StubbedFeature}

class FeedbackNotSignedInFeature extends StubbedFeature with NavigationSteps with ApiSteps with ObservationSteps {


  Feature("Feedback about the beta when not signed in") {

    info("In order to make my views known about the beta")
    info("As an unauthenticated user")
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


    Scenario("The referrer URL is sent to Deskpro") {
      Given("I come from a page that links to the beta feedback")
      go to ExternalPage
      ExternalPage.clickOnFeedbackLink()

      When("I fill the feedback form correctly")
      val page = new UnauthenticatedFeedbackPage
      page.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I send the feedback form")
      page.submitFeedbackForm()

      Then("the Deskpro endpoint '/deskpro/feedback' has received the following POST request:")
      verify_post(to = "/deskpro/feedback", body =
        s"""
          |{
          |   "name":"$Name",
          |   "email":"$Email",
          |   "subject":"Beta feedback submission",
          |   "rating":"1",
          |   "message":"$Comment",
          |   "referrer":"http://localhost:11111/external/page",
          |   "javascriptEnabled":"Y",
          |   "authId":"n/a",
          |   "areaOfTax":"n/a",
          |   "sessionId":"n/a",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }


    Scenario("All fields are mandatory") {
      When("I don't fill the form")
      val page = new UnauthenticatedFeedbackPage

      And("I try to send the feedback form")
      page.submitFeedbackForm()

      Then("I am on the 'Send your feedback' page")
      i_am_on_the_page("Send your feedback")

      And("I see:")
      i_see(
        "Tell us what you think of the service.",
        "Please provide your name.",
        "Enter a valid email address.",
        "Enter your comments."
      )

      And("the Deskpro enpoint '/deskpro/feedback' has not been hit")
      verify_post_no_hit("/deskpro/feedback")
    }


    Scenario("Fields have a size limit") {
      Given("the 'name' cannot be longer than 70 characters")

      And("the 'email' cannot be longer than 255 characters")

      And("the 'comment' cannot be longer than 2000 characters")

      When("I fill the form with values that are too long")
      val page = new UnauthenticatedFeedbackPage
      page.fillOutFeedbackForm(1, TooLongName, TooLongEmail, TooLongComment)


      And("I try to send the feedback form")
      page.submitFeedbackForm()

      Then("I am on the 'Send your feedback' page")
      i_am_on_the_page("Send your feedback")

      And("I see:")
      i_see(
      "Your name cannot be longer than 70 characters",
      "The email cannot be longer than 255 characters",
      "The comment cannot be longer than 2000 characters"
      )
    }


    Scenario("Invalid email address") {
      When("I fill the form with an invalid email address")
      val page = new UnauthenticatedFeedbackPage
      page.fillOutFeedbackForm(1, Name, InvalidEmailAddress, Comment)

      And("I try to send the feedback form")
      page.submitFeedbackForm()

      Then("I am on the 'Send your feedback' page")
      i_am_on_the_page("Send your feedback")

      And("I see:")
      i_see("Enter a valid email address")

    }


    Scenario("Call to Deskpro times out after several seconds") {
      Given("the call to Deskpro endpoint '/deskpro/feedback' will take too much time")
      service_will_return_payload_for_POST_request("/deskpro/feedback", delayMillis = 10000)("")

      When("I fill the feedback form correctly")
      val page = new UnauthenticatedFeedbackPage
      page.fillOutFeedbackForm(1, Name, Email, Comment)


      And("I try to send the feedback form")
      page.submitFeedbackForm()

      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
      i_am_on_the_page("Sorry, we’re experiencing technical difficulties")

      And("I see:")
      i_see("Please try again in a few minutes.")
    }



    val statuses = Seq("404", "500")

    statuses foreach { status =>
      Scenario(s"Deskpro fails with status $status") {
        Given(s"the call to Deskpro endpoint '/deskpro/feedback' will fail with status $status")
        service_will_fail_on_POST_request("/deskpro/feedback", status.toInt)

        When("I fill the contact form correctly")
        val page = new UnauthenticatedFeedbackPage
        page.fillOutFeedbackForm(1, Name, Email, Comment)

        And("I try to send the feedback form")
        page.submitFeedbackForm()

        Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
        i_am_on_the_page("Sorry, we’re experiencing technical difficulties")

        And("I see:")
        i_see("Please try again in a few minutes.")
      }
    }

  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"

  private val InvalidEmailAddress = "grumpycarebears.com"

  private val TooLongName = "G"*71
  private val TooLongEmail = "g"*255 + "@x.com"
  private val TooLongComment = "I"*2001
}
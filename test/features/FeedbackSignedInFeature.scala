package features

import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import support.page.AuthenticatedFeedbackPage
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs.{Login, StubbedFeature}

class FeedbackSignedInFeature extends StubbedFeature with NavigationSteps with ApiSteps with ObservationSteps  {


  Feature("Feedback about the beta when signed in") {

    info("In order to make my views known about the beta")
    info("As an authenticated user")
    info("I want to leave my feedback")


    Background {
      Given("I go to the 'Send your feedback' page")
      go to new AuthenticatedFeedbackPage
      i_am_on_the_page("Send your feedback")

    }


    Scenario("Submit feedback successfully") {
      When("I fill the feedback form correctly")
      val page = new AuthenticatedFeedbackPage
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
          |   "authId":"/auth/oid/1234567890",
          |   "areaOfTax":"biztax",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }


  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"

}
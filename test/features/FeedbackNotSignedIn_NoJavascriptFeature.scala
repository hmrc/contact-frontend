package features

import org.skyscreamer.jsonassert.JSONCompareMode
import support.behaviour.NavigationSugar
import support.page.{FeedbackSuccessPage, UnauthenticatedFeedbackPage}
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs._


class FeedbackNotSignedIn_NoJavascriptFeature extends NoJsFeature with NavigationSugar with ApiSteps with ObservationSteps {

  Feature("Feedback about the beta when not signed and with Javascript disabled") {

    info("In order to make my views known about the beta")
    info("As an unauthenticated user with Javascript disabled")
    info("I want to leave my feedback")


    Background {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)
    }


    Scenario("Submit feedback successfully") {
      When("I fill the feedback form correctly")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I am on the 'Your feedback' page")
      on(FeedbackSuccessPage)

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
          |   "javascriptEnabled":"N",
          |   "authId":"n/a",
          |   "areaOfTax":"n/a",
          |   "sessionId":"n/a",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, JSONCompareMode.LENIENT)
    }
  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"
}
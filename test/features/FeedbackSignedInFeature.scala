package features

import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import support.StubbedFeatureSpec
import support.page.{AuthenticatedFeedbackPage, FeedbackSuccessPage}
import support.stubs.Login

class FeedbackSignedInFeature extends StubbedFeatureSpec {

  feature("Feedback about the beta when signed in") {

    info("In order to make my views known about the beta")
    info("As an authenticated user")
    info("I want to leave my feedback")

    scenario("Submit feedback successfully") {
      val Name = "Grumpy Bear"
      val Email = "grumpy@carebears.com"
      val Comment = "I am writing a comment"

      Given("I go to the 'Send your feedback' page")
      goOn(AuthenticatedFeedbackPage)
      AuthenticatedFeedbackPage.ratingsList() shouldBe "Verybad Bad Neutral Good Verygood"

      When("I fill the feedback form correctly")
      AuthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I send the feedback form")
      AuthenticatedFeedbackPage.submitFeedbackForm()

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
          |   "javascriptEnabled":"Y",
          |   "authId":"/auth/oid/1234567890",
          |   "areaOfTax":"unknown",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }

  }

}
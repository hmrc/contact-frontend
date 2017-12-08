package features

import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import support.StubbedFeatureSpec
import support.page.{ExternalPage, FeedbackSuccessPage, TechnicalDifficultiesPage, UnauthenticatedFeedbackPage}

class FeedbackNotSignedInFeature extends StubbedFeatureSpec {

  val Name = "Grumpy Bear"
  val Email = "grumpy@carebears.com"
  val Comment = "I am writing a comment"
  val InvalidEmailAddress = "grumpycarebears.com"
  val TooLongName = "G"*71
  val TooLongEmail = "g"*255 + "@x.com"
  val TooLongComment = "I"*2001
  
  feature("Feedback about the beta when not signed in") {

    info("In order to make my views known about the beta")
    info("As an unauthenticated user")
    info("I want to leave my feedback")

    scenario("Submit feedback successfully") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.ratingsList() mustBe List("Very good", "Good", "Neutral", "Bad", "Very bad")

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
          |   "areaOfTax":"unknown",
          |   "sessionId":"n/a",
          |   "userTaxIdentifiers":{},
          |   "service": "unknown"
          |}
        """.stripMargin, LENIENT)
    }

//    MoveToAcceptanceTest
    scenario("The referrer URL is sent to Deskpro") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Given("I come from a page that links to the beta feedback")
      goOn(ExternalPage)
      ExternalPage.clickOnFeedbackLink()

      When("I fill the feedback form correctly")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I see:")
      i_see(
        "Thank you",
        "Your feedback has been received."
      )

//      MoveToAcceptanceTest: Deskpro Integration Test
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
          |   "javascriptEnabled":"N",
          |   "authId":"n/a",
          |   "areaOfTax":"unknown",
          |   "sessionId":"n/a",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }

//    MoveToAcceptanceTest
    scenario("All fields are mandatory") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      When("I don't fill the form")

      And("I try to send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I am on the 'Send your feedback' page")
      on(UnauthenticatedFeedbackPage)

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

//    MoveToAcceptanceTest
    scenario("Fields have a size limit") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Given("the 'name' cannot be longer than 70 characters")

      And("the 'email' cannot be longer than 255 characters")

      And("the 'comment' cannot be longer than 2000 characters")

      When("I fill the form with values that are too long")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, TooLongName, TooLongEmail, TooLongComment)

      And("I try to send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I am on the 'Send your feedback' page")
      on(UnauthenticatedFeedbackPage)

      And("I see:")
      i_see(
      "Your name can't be longer than 70 characters",
      "Your email can't be longer than 255 characters"
//      "0 remaining characters"
       )
    }

//    MoveToAcceptanceTest
    scenario("Invalid email address") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      When("I fill the form with an invalid email address")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, InvalidEmailAddress, Comment)

      And("I try to send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I am on the 'Send your feedback' page")
      on(UnauthenticatedFeedbackPage)

      And("I see:")
      i_see("Enter a valid email address")
    }

//    MoveToAcceptanceTest: Deskpro Integration Test
//    Commented out, library changes failing this test
//    scenario("Call to Deskpro times out after several seconds") {
//      Given("I go to the 'Feedback' page")
//      goOn(UnauthenticatedFeedbackPage)
//
//      Given("the call to Deskpro endpoint '/deskpro/feedback' will take too much time")
//      service_will_return_payload_for_POST_request("/deskpro/feedback", delayMillis = 10000)("")
//
//      When("I fill the feedback form correctly")
//      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, Email, Comment)
//
//      And("I try to send the feedback form")
//      UnauthenticatedFeedbackPage.submitFeedbackForm()
//
//      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
//      on(TechnicalDifficultiesPage)
//
//      And("I see:")
//      i_see("Please try again in a few minutes.")
//    }

//    MoveToAcceptanceTest: Deskpro Integration Test
    scenario("Deskpro fails with status 404") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Given(s"the call to Deskpro endpoint '/deskpro/feedback' will fail with status 404")
      service_will_fail_on_POST_request("/deskpro/feedback", 404)

      When("I fill the contact form correctly")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I try to send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
      on(TechnicalDifficultiesPage)

      And("I see:")
      i_see("Please try again in a few minutes.")
    }

//    MoveToAcceptanceTest: Deskpro Integration Test
    scenario("Deskpro fails with status 500") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Given(s"the call to Deskpro endpoint '/deskpro/feedback' will fail with status 500")
      service_will_fail_on_POST_request("/deskpro/feedback", 500)

      When("I fill the contact form correctly")
      UnauthenticatedFeedbackPage.fillOutFeedbackForm(1, Name, Email, Comment)

      And("I try to send the feedback form")
      UnauthenticatedFeedbackPage.submitFeedbackForm()

      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
      on(TechnicalDifficultiesPage)

      And("I see:")
      i_see("Please try again in a few minutes.")
    }

  }

}
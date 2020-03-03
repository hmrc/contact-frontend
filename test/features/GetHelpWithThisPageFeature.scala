package features

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.ws.ahc.AhcWSClient
import play.api.test.Helpers
import support.StubbedFeatureSpec
import support.page.UnauthenticatedFeedbackPage

class GetHelpWithThisPageFeature extends StubbedFeatureSpec with ScalaFutures {

  override def useJavascript: Boolean = true

  val Name = "Grumpy Bear"
  val Email = "grumpy@carebears.com"
  val WhatWhereYouDoing = "Something"
  val WhatDoYouNeedHelpWith = "Nothing"

  feature("Get help with this page form") {

    info("In order to get help with a specific page")
    info("As a tax payer")
    info("I want to ask for help to HMRC")

    scenario("Successful form submission") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      When("I fill the Get Help with this page' form correctly")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.fillProblemReport(Name, Email, WhatWhereYouDoing, WhatDoYouNeedHelpWith)

      And("I send the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.submitProblemReport()

      Then("I remain on the same page")
      on(UnauthenticatedFeedbackPage)

      Then("I see:")
      i_see(
        "Thank you",
        "Someone will get back to you within 2 working days."
      )
    }

//  MoveToAcceptanceTest
    scenario("External posts to form are not allowed") {
      When("I post to the form using external rest client")
      val baseUrl = s"http://localhost:${Helpers.testServerPort}"

      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val wsClient              = AhcWSClient()
      val wsRequest            = wsClient.url(s"$baseUrl/contact/problem_reports_secure")

      val eventualResponse     = wsRequest
        .withHttpHeaders(("Content-Type", "application/x-www-form-urlencoded"))
        .post(s"report-name=Mike&report-email=mike@example.com&report-action=Action&report-error=error")

      And("I get a bad request error")
      eventualResponse.futureValue.status mustBe 403
      eventualResponse.futureValue.body must include("No CSRF token found in headers")
    }

//    MoveToAcceptanceTest
    scenario("The problem report form toggles") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Then("The get 'Get help with this page' form is hidden")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.problemReportHidden must be (true)

      When("I open the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport
      i_see("Get help using this service")

      Then("The 'Get help with this page' form is visible")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.problemReportHidden must be (false)

      When("I close the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      Then("The get 'Get help with this page' form is hidden")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.problemReportHidden must be (true)
    }

//    MoveToAcceptanceTest
    scenario("Invalid name error if you entered anything other than letters (lower and upper case), space, comma, period, braces and hyphen") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport
      i_see("Get help using this service")

      When("I fill in an invalid name")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.typeName("<")

      And("Change focus to trigger validation")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.typeEmail("some@validemail.com")

      Then("I see an error for invalid name")
      i_see("Letters or punctuation only please")
    }

//    MoveToAcceptanceTest
    scenario("Invalid email error if you entered a badly formed email") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport
      i_see("Get help using this service")

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.typeEmail("not@valid.")

      And("Change focus to trigger validation")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.typeName("Validname")

      Then("I see an error for invalid email")
      i_see("Please enter a valid email address.")
    }

//    MoveToAcceptanceTest
    scenario("All fields are mandatory") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport
      i_see("Get help using this service")

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.clickSubmitButton()

      Then("I see an error for invalid name")
      i_see(
        "Please provide your name.",
        "Please provide your email address.",
        "Please enter details of what you were doing.",
        "Please enter details of what went wrong.")
    }
  }


}
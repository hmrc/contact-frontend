package features

import com.ning.http.client.AsyncHttpClientConfig
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.selenium.WebBrowser.go
import org.skyscreamer.jsonassert.JSONCompareMode._
import play.api.libs.ws.WS
import play.api.libs.ws.ning.NingWSClient
import support.behaviour.NavigationSugar
import support.page.UnauthenticatedFeedbackPage
import support.steps.{ObservationSteps, ApiSteps, NavigationSteps}
import support.stubs.StubbedFeature

class GetHelpWithThisPageFeature extends StubbedFeature with ScalaFutures with IntegrationPatience with NavigationSugar with ObservationSteps {


  Feature("Get help with this page form") {

    info("In order to get help with a specific page")
    info("As a tax payer")
    info("I want to ask for help to HMRC")


    Scenario("Successful form submission") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      When("I fill the Get Help with this page' form correctly")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.fillProblemReport(Name, Email, WhatWhereYouDoing, WhatDoYouNeedHelpWith)

      And("I send the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.submitProblemReport()

      Then("I remain on the same page")
      on(UnauthenticatedFeedbackPage)

      Then("I see:")
      i_see(
        "Thank you",
        "Your message has been sent, and the team will get back to you within 2 working days."
      )
    }


    Scenario("External posts to form are not allowed") {
      When("I post to the form using external rest client")
      val baseUrl = "http://localhost:9000"
      implicit val wsClient = new NingWSClient(new AsyncHttpClientConfig.Builder().build())

      val eventualResponse = WS.clientUrl(s"$baseUrl/contact/problem_reports")
        .withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
        .post(s"report-name=Mike&report-email=mike@example.com&report-action=Action&report-error=error")

      And("I get a bad request error")
      eventualResponse.futureValue.status shouldBe 403
      eventualResponse.futureValue.body shouldBe "No CSRF token found in headers"
    }

    Scenario("The problem report form toggles") {
      Given("I go to the 'Feedback' page")
      goOn(UnauthenticatedFeedbackPage)

      Then("The get 'Get help with this page' form is hidden")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.problemReportHidden should be (true)

      When("I open the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      Then("The 'Get help with this page' form is visible")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.problemReportHidden should be (false)

      When("I close the 'Get help with this page' form")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      Then("The get 'Get help with this page' form is hidden")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.problemReportHidden should be (true)
    }

    Scenario("Invalid name error if you entered anything other than letters (lower and upper case), space, comma, period, braces and hyphen") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.typeName("<")

      Then("I see an error for invalid name")
      UnauthenticatedFeedbackPage.bodyText should include ("Letters or punctuation only please")
    }

    Scenario("Invalid email error if you entered a badly formed email") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.typeEmail("not@valid.")

      Then("I see an error for invalid email")
      UnauthenticatedFeedbackPage.bodyText should include ("Please enter a valid email address.")
    }

    Scenario("All fields are mandatory") {
      Given("I have the 'Get help with this page' form open")
      goOn(UnauthenticatedFeedbackPage)
      UnauthenticatedFeedbackPage.getHelpWithThisPage.toggleProblemReport

      When("I fill in an invalid email address")
      UnauthenticatedFeedbackPage.getHelpWithThisPage.clickSubmitButton()

      Then("I see an error for invalid name")
      UnauthenticatedFeedbackPage.bodyText should include ("Please provide your name.")
      UnauthenticatedFeedbackPage.bodyText should include ("Please provide your email address.")
      UnauthenticatedFeedbackPage.bodyText should include ("Please enter details of what you were doing.")
      UnauthenticatedFeedbackPage.bodyText should include ("Please enter details of what went wrong.")
    }
  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val WhatWhereYouDoing = "Something"
  private val WhatDoYouNeedHelpWith = "Nothing"

}
package features

import com.ning.http.client.AsyncHttpClientConfig
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.selenium.WebBrowser.go
import org.skyscreamer.jsonassert.JSONCompareMode._
import play.api.libs.ws.WS
import play.api.libs.ws.ning.NingWSClient
import support.page.UnauthenticatedFeedbackPage
import support.steps.{ObservationSteps, ApiSteps, NavigationSteps}
import support.stubs.StubbedFeature

class GetHelpWithThisPageFeature extends StubbedFeature with ScalaFutures with IntegrationPatience with NavigationSteps with ObservationSteps {


  Feature("Get help with this page form") {

    info("In order to get help with a specific page")
    info("As a tax payer")
    info("I want to ask for help to HMRC")



    Scenario("Successful form submission") {
      Given("I go to the 'Feedback' page")
      go to new UnauthenticatedFeedbackPage
      i_am_on_the_page("Send your feedback")

      When("I fill the Get Help with this page' form correctly")
      val page = new UnauthenticatedFeedbackPage
      page.GetHelpWithThisPage.fillProblemReport(Name, Email, WhatWhereYouDoing, WhatDoYouNeedHelpWith)

      And("I send the 'Get help with this page' form")
      page.GetHelpWithThisPage.submitProblemReport()

      Then("I remain on the same page")
      i_am_on_the_page("Send your feedback")

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


  }

  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val WhatWhereYouDoing = "Something"
  private val WhatDoYouNeedHelpWith = "Nothing"

}
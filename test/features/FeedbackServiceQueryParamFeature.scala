package features

import org.openqa.selenium.WebDriver
import support.StubbedFeatureSpec
import support.page.UnauthenticatedFeedbackPageWithServiceQueryParameter
import support.steps.Env

class FeedbackServiceQueryParamFeature extends StubbedFeatureSpec {

  val testUsingWebDriver: WebDriver = Env.getDriverWithJS

  feature("Feedback form service field populated when passed as a query parameter") {

    info("As someone handling deskpro tickets")
    info("I want a service against the ticket")
    info("So that I can assign it to the correct department")

    scenario("Service field populated when passed") {
      When("I go to the unauthenticated feedback page with a service query parameter of YTA")
      goOn(UnauthenticatedFeedbackPageWithServiceQueryParameter)

      Then("The form contains the service value YTA")
      UnauthenticatedFeedbackPageWithServiceQueryParameter.serviceFieldValue() shouldBe Some("YTA")
    }
  }
}

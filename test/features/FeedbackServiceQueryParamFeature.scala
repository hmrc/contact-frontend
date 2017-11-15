package features

import support.StubbedFeatureSpec
import support.page.UnauthenticatedFeedbackPageWithServiceQueryParameter

class FeedbackServiceQueryParamFeature extends StubbedFeatureSpec {

  feature("Feedback form service field populated when passed as a query parameter") {

    info("As someone handling deskpro tickets")
    info("I want a service against the ticket")
    info("So that I can assign it to the correct department")

//    MoveToAcceptanceTest
    scenario("Service field populated when passed") {
      When("I go to the unauthenticated feedback page with a service query parameter of YTA")
      goOn(UnauthenticatedFeedbackPageWithServiceQueryParameter)

      Then("The form contains the service value YTA")
      UnauthenticatedFeedbackPageWithServiceQueryParameter.serviceFieldValue() shouldBe Some("YTA")
    }
  }
}

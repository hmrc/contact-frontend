package features

import support.behaviour.NavigationSugar
import support.page.ContactHmrcPartialPage
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs.StubbedFeature

class ContactHmrcPartialFeature extends StubbedFeature with NavigationSugar with NavigationSteps with ApiSteps with ObservationSteps {

  Feature("Contact HMRC partial") {

    info("In order to include the Contact HMRC form in other applications")
    info("As a Application")
    info("I need to be able to submit contact form partial")

    Scenario("Contact form retrieved successfully") {
      goOn(new ContactHmrcPartialPage("http://server/account/contact", Some("myservice")))

      Then("I confirm the submit URL is correct")
      tagName("form").element.attribute("action") shouldBe Some("http://server/account/contact")
      name("csrfToken").element.attribute("value") shouldBe Some("token")
      name("service").element.attribute("value") shouldBe Some("myservice")
    }

  }
}

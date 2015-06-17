package features

import support.StubbedFeatureSpec
import support.page.ContactHmrcPartialPage

class ContactHmrcPartialFeature extends StubbedFeatureSpec {

  feature("Contact HMRC partial") {

    info("In order to include the Contact HMRC form in other applications")
    info("As a Application")
    info("I need to be able to submit contact form partial")

    scenario("Contact form retrieved successfully") {
      goOn(new ContactHmrcPartialPage("http://server/account/contact", Some("myservice")))

      Then("I confirm the submit URL is correct")
      tagName("form").element.attribute("action") shouldBe Some("http://server/account/contact")
      name("csrfToken").element.attribute("value") shouldBe Some("token")
      name("service").element.attribute("value") shouldBe Some("myservice")
    }

  }
}

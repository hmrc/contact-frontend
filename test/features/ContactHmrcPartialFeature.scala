package features

import support.StubbedFeatureSpec
import support.page.{ContactHmrcPage, ContactHmrcPartialPage}

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

    scenario("Copy hidden when renderFormOnly set to true") {
      go(new ContactHmrcPartialPage(submitUrl = "http://server/account/contact", renderFormOnly = Some(true)))

      Then("The form is rendered")
      tagName("form").element.attribute("action") shouldBe Some("http://server/account/contact")

      And("I do not see any copy with the form")
      ContactHmrcPage.bodyText.contains("If you have a specific tax query") shouldBe false
      ContactHmrcPage.bodyText.contains("How can we help you?") shouldBe false
    }

  }
}

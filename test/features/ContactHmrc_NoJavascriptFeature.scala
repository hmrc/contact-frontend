package features

import org.skyscreamer.jsonassert.JSONCompareMode
import support.StubbedFeatureSpec
import support.page.ContactHmrcPage
import support.steps.Env
import support.stubs.Login

class ContactHmrc_NoJavascriptFeature extends StubbedFeatureSpec {

  feature("Contact HMRC with Javascript disabled") {

    info("In order to make my views known")
    info("As a Tax Payer with Javascript disabled in my browser")
    info("I want to contact HMRC")

    scenario("Contact form sent successfully") {

      val Name = "Grumpy Bear"
      val Email = "grumpy@carebears.com"
      val Comment = "I am writing a comment"

      Given("JavaScript is disabled")
      Env.disableJavascript()

      And("I go to the 'Help' page")
      goOn(ContactHmrcPage)

      When("I fill the contact form correctly")
      ContactHmrcPage.fillContactForm(Name, Email, Comment)

      And("I send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I see:")
      i_see("Thank you",
        "Someone will get back to you within 2 working days.")

      And("the Deskpro endpoint '/deskpro/ticket' has received the following POST request:")
      verify_post(to = "/deskpro/ticket", body =
        s"""
          |{
          |   "name":"$Name",
          |   "email":"$Email",
          |   "subject":"Contact form submission",
          |   "message":"$Comment",
          |   "referrer":"n/a",
          |   "javascriptEnabled":"N",
          |   "authId":"/auth/oid/1234567890",
          |   "areaOfTax":"unknown",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, JSONCompareMode.LENIENT)
    }

  }

}
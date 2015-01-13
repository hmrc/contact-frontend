package features

import org.skyscreamer.jsonassert.JSONCompareMode
import support.behaviour.NavigationSugar
import support.page.ContactHmrcPage
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs.{NoJsFeature, Login, StubbedFeature}

class ContactHmrc_NoJavascriptFeature extends NoJsFeature with NavigationSugar with ApiSteps with ObservationSteps {

  Feature("Contact HMRC with Javascript disabled") {

    info("In order to make my views known")
    info("As a Tax Payer with Javascript disabled in my browser")
    info("I want to contact HMRC")


    Background {
      Given("I am logged in")

      And("I go to the 'Help' page")
      goOn(ContactHmrcPage)
    }

    Scenario("Contact form sent successfully") {
      When("I fill the contact form correctly")
      ContactHmrcPage.fillContactForm(Name, Email, Comment)

      And("I send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I see:")
      i_see("Thank you",
        "Your message has been sent, and the team will get back to you within 2 working days.")

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
          |   "areaOfTax":"biztax",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, JSONCompareMode.LENIENT)
    }

  }


  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"
}
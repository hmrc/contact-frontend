package features

import org.skyscreamer.jsonassert.JSONCompareMode._
import support.behaviour.NavigationSugar
import support.page.{TechnicalDifficultiesPage, ContactHmrcPage, ExternalPage}
import support.steps.{NavigationSteps, ApiSteps, ObservationSteps}
import support.stubs.{Login, StubbedFeature}

class ContactHmrcFeature extends StubbedFeature with NavigationSugar with NavigationSteps with ApiSteps with ObservationSteps {

  Feature("Help") {

    info("In order to make my views known")
    info("As a Tax Payer")
    info("I need Help")


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
          |   "javascriptEnabled":"Y",
          |   "authId":"/auth/oid/1234567890",
          |   "areaOfTax":"biztax",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }

    Scenario("All fields are mandatory") {
      When("I fill the form with empty values")

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I am on the 'Help' page")
      on(ContactHmrcPage)

      And("I see:")
      i_see(
        "Please provide your name",
        "Enter a valid email address",
        "Enter your comments")

      And("the Deskpro endpoint '/deskpro/ticket' has not been hit")
      verify_post_no_hit("/deskpro/ticket")
    }


    Scenario("Fields have a size limit") {
      Given("the 'name' cannot be longer than 70 characters")
      And("the 'email' cannot be longer than 255 characters")
      And("the 'comment' cannot be longer than 2000 characters")

      When("I fill the contact form with values that are too long")
      ContactHmrcPage.fillContactForm(TooLongName, TooLongEmail, TooLongComment)

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I am on the 'Help' page")
      on(ContactHmrcPage)

      And("I see:")
      i_see(
        "Your name cannot be longer than 70 characters",
        "The email cannot be longer than 255 characters",
        "The comment cannot be longer than 2000 characters")

      And("the Deskpro endpoint '/deskpro/ticket' has not been hit")
      verify_post_no_hit("/deskpro/ticket")
    }


    Scenario("Invalid email address")  {
      ContactHmrcPage.fillContactForm(Name, InvalidEmailAddress, Comment)

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I am on the 'Help' page")
      on(ContactHmrcPage)

      And("I see:")

      pendingUntilFixed(i_see(
        "Enter a valid email address"))

      And("the Deskpro endpoint '/deskpro/ticket' has not been hit")
      verify_post_no_hit("/deskpro/ticket")
    }

    Scenario("Deskpro fails with 404") {
      Given("the call to Deskpro endpoint '/deskpro/ticket' will fail with status 404")
      service_will_fail_on_POST_request("/deskpro/ticket", 404)

      When("I fill the contact form correctly")
      ContactHmrcPage.fillContactForm(Name, Email, Comment)

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
      on(TechnicalDifficultiesPage)

      And("I see:")
      i_see("There was a problem sending your query.")
    }

    Scenario("Deskpro times out") {
      Given("the call to Deskpro endpoint '/deskpro/ticket' will take too much time")
      service_will_return_payload_for_POST_request("/deskpro/ticket", delayMillis = 10000)("")

      When("I fill the contact form correctly")
      ContactHmrcPage.fillContactForm(Name, Email, Comment)

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
      on(TechnicalDifficultiesPage)

      And("I see:")
      i_see("There was a problem sending your query.")
    }

    Scenario("Deskpro fails with 500") {
      Given("the call to Deskpro endpoint '/deskpro/ticket' will fail with status 500")
      service_will_fail_on_POST_request("/deskpro/ticket", 500)

      When("I fill the contact form correctly")
      ContactHmrcPage.fillContactForm(Name, Email, Comment)

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("I am on the 'Sorry, we’re experiencing technical difficulties' page")
      on(TechnicalDifficultiesPage)

      And("I see:")
      i_see("There was a problem sending your query.")
    }


    Scenario("Link to contact HMRC about tax queries") {
      When("I click on the 'contact HMRC' link")
      ContactHmrcPage.clickOnContactHmrcLink()

      Then("another tab is opened")
      another_tab_is_opened()
      switch_tab()

      And("I am on the 'Contact us' page")
      i_am_on_the_page("HM Revenue & Customs Contacts")
    }


    Scenario("The referrer URL is sent to Deskpro") {
      Given("I come from a page that links to Contact HMRC")
      goOn(ExternalPage)
      ExternalPage.clickOnContactHmrcLink()

      When("I fill the contact form correctly")
      ContactHmrcPage.fillContactForm(Name, Email, Comment)

      And("I try to send the contact form")
      ContactHmrcPage.submitContactForm()

      Then("the Deskpro endpoint '/deskpro/ticket' has received the following POST request:")
      verify_post(to = "/deskpro/ticket", body =
        s"""
          |{
          |   "name":"$Name",
          |   "email":"$Email",
          |   "subject":"Contact form submission",
          |   "message":"$Comment",
          |   "referrer":"http://localhost:11111/external/page",
          |   "javascriptEnabled":"Y",
          |   "authId":"/auth/oid/1234567890",
          |   "areaOfTax":"biztax",
          |   "sessionId": "${Login.SessionId}",
          |   "userTaxIdentifiers":{}
          |}
        """.stripMargin, LENIENT)
    }
  }


  private val Name = "Grumpy Bear"
  private val Email = "grumpy@carebears.com"
  private val Comment = "I am writing a comment"

  private val InvalidEmailAddress = "grumpycarebears.com"

  private val TooLongName = "G"*71
  private val TooLongEmail = "g"*255 + "@x.com"
  private val TooLongComment = "I"*2001
}

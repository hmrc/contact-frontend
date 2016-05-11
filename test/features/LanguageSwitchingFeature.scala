package features

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.i18n.Messages
import play.api.libs.json.{JsString, Json}
import support.StubbedFeatureSpec
import support.page.SurveyPage._
import support.page.{SurveyPageWithTicketAndServiceIds, SurveyConfirmationPage}

class LanguageSwitchingFeature extends StubbedFeatureSpec {

  feature("Language Switching") {

    scenario("Switch from English to Welsh in the survey") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      And("I click on the switch language link")
      click on linkText("Cymraeg")

      i_see(Messages("SURVEY"))
      i_see(Messages("HOW SATISFIED ARE YOU WITH THE ANSWER WE GAVE YOU?"))
      i_see(Messages("HOW SATISFIED ARE YOU WITH THE SPEED OF OUR REPLY?"))
      i_see(Messages("TELL US HOW WE CAN IMPROVE THE SUPPORT WE GIVE YOU."))
      i_see(Messages("2500 CHARACTERS OR LESS"))
      i_see(Messages("GET HELP WITH THIS PAGE."))
      i_see(Messages("English"))

      And("I click on the switch language link")
      click on linkText("English")

      i_see(Messages("Survey"))
      i_see(Messages("How satisfied are you with the answer we gave you?"))
      i_see(Messages("How satisfied are you with the speed of our reply?"))
      i_see(Messages("Tell us how we can improve the support we give you."))
      i_see(Messages("2500 characters or less"))
      i_see(Messages("Get help with this page."))
      i_see(Messages("Cymraeg"))
    }

    scenario("Show successfully sent message whilst rejecting feedback when ticket ref is invalid") {

      val invalidTicketId = "HMRC-Z2V6!UK5"

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds(invalidTicketId,"arbitrary%20service%20id"))

      When("I successfully fill in the form")
      selectHowHelpfulTheResponseWas("strongly-agree")
      selectHowSpeedyTheResponseWas("strongly-agree")
      setAdditionalComment("Your mother is an 'amster and your father smelled of elderberry!")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should get sent to 'audit land'")
      val loggedRequests = getDatastreamSubmissionsForSurvey()
      loggedRequests.size() shouldBe 0
    }

    scenario("Show successfully sent message whilst rejecting feedback when ticket ref is empty") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("","arbitrary%20service%20id"))

      When("I successfully fill in the form")
      selectHowHelpfulTheResponseWas("strongly-agree")
      selectHowSpeedyTheResponseWas("strongly-agree")
      setAdditionalComment("Damn you Artheur King and your silly English Kniiiights!")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should get sent to 'audit land'")
      val loggedRequests = getDatastreamSubmissionsForSurvey()
      loggedRequests.size() shouldBe 0
    }

    scenario("Show successfully sent message whilst rejecting feedback when service is empty") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6AUK5",""))

      When("I successfully fill in the form")
      selectHowHelpfulTheResponseWas("strongly-agree")
      selectHowSpeedyTheResponseWas("strongly-agree")
      setAdditionalComment("Your mother is a hamster and your father smelled of elderberry!")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should get sent to 'audit land'")
      val loggedRequests = getDatastreamSubmissionsForSurvey()
      loggedRequests.size() shouldBe 0
    }

    scenario("Survey form errors, but still shows confirmation page") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(500)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      When("I successfully fill in the form")
      selectHowHelpfulTheResponseWas("strongly-agree")
      selectHowSpeedyTheResponseWas("strongly-agree")
      setAdditionalComment("Blah blooh blah la dee daaaaa")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should not get sent to 'audit land' - but it does")
      val loggedRequests = getDatastreamSubmissionsForSurvey()
      loggedRequests.size() shouldBe 1

      And("I should see the failure page, but i cant determine a failure has occurred, so i show the conf page. lovely.")
      on(SurveyConfirmationPage)
    }

    scenario("Survey submitted with no radio button selections, but still shows confirmation page") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(500)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      When("I fill the form in without selecting radio button options")
      setAdditionalComment("Blah blooh blah la dee daaaaa")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should not get sent to 'audit land' - but it does")
      val loggedRequests = getDatastreamSubmissionsForSurvey()
      val resultJsonString = loggedRequests.get(0).getBodyAsString
      val resultJson = Json.parse(resultJsonString)
      (resultJson \ "detail" \ "helpful").asInstanceOf[JsString].value   shouldBe("0")
      (resultJson \ "detail" \ "speed").asInstanceOf[JsString].value     shouldBe("0")
      (resultJson \ "detail" \ "ticketId").asInstanceOf[JsString].value  shouldBe("HMRC-Z2V6DUK5")
      (resultJson \ "detail" \ "serviceId").asInstanceOf[JsString].value  shouldBe("arbitrary service id")
      (resultJson \ "detail" \ "improve").asInstanceOf[JsString].value   shouldBe("Blah blooh blah la dee daaaaa")

      And("I should see the failure page, but i cant determine a failure has occurred, so i show the conf page. lovely.")
      on(SurveyConfirmationPage)
    }

    scenario("Survey form errors, but still shows confirmation page again") {

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      When("I successfully fill in the form")
      selectHowHelpfulTheResponseWas("strongly-agree")
      selectHowSpeedyTheResponseWas("strongly-agree")
      setAdditionalComment("Blah blooh blah la dee daaaaa")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should not get sent to 'audit land' - but it does")
      val loggedRequests = getDatastreamSubmissionsForSurvey()
      loggedRequests.size() shouldBe 1

      And("I should see the failure page, but i cant determine a failure has occurred, so i show the conf page. lovely.")
      on(SurveyConfirmationPage)
    }
  }

  def getDatastreamSubmissionsForSurvey() = WireMock.findAll(postRequestedFor(urlEqualTo("/write/audit"))
    .withRequestBody(matching(".*DeskproSurvey.*")))
}

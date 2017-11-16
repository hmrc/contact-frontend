package features

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import support.StubbedFeatureSpec
import support.page.SurveyPage._
import support.page.{SurveyConfirmationPage, SurveyPageWithTicketAndServiceIds}

@RunWith(classOf[JUnitRunner])
class SurveyFeature extends StubbedFeatureSpec {

  feature("Survey") {

    scenario("Survey form sent successfully") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
        goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      When("I successfully fill in the form")
        selectHowHelpfulTheResponseWas("strongly-agree")
        selectHowSpeedyTheResponseWas("strongly-agree")
        setAdditionalComment("Blah blooh blah la dee daaaaa")

      And("Submit the form")
        clickSubmitButton()

      Then("The data should get sent to 'audit land'")
      shouldSendSurveyToDatastream

      val loggedRequests = getDatastreamSubmissionsForSurvey

      val json = Json.parse(loggedRequests.get(0).getBodyAsString)

      def fieldShouldBe(key: String, expectedValue: String) = (json \ "detail" \ key).asOpt[String] shouldBe Some(expectedValue)

      fieldShouldBe("helpful", "5")
      fieldShouldBe("speed", "5")
      fieldShouldBe("improve", "Blah blooh blah la dee daaaaa")
      fieldShouldBe("ticketId", "HMRC-Z2V6DUK5")
      fieldShouldBe("serviceId", "arbitrary service id")


      And("I should see the confirmation page - happy days")
        on(SurveyConfirmationPage)
    }

//    MoveToAcceptanceTest
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

      Then("The data should not get sent to 'audit land'")
      shouldNotSendSurveyToDatastream
    }

//    MoveToAcceptanceTest
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

      Then("The data should not get sent to 'audit land'")
      shouldNotSendSurveyToDatastream
    }

//    MoveToAcceptanceTest
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

      Then("The data should not get sent to 'audit land'")
      shouldNotSendSurveyToDatastream
    }

//    MoveToAcceptanceTest
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

      Then("The data should get sent to 'audit land' - though this is questionable behaviour from the code")
      shouldSendSurveyToDatastream

      And("I should see the failure page, but i cant determine a failure has occurred, so i show the conf page. lovely.")
        on(SurveyConfirmationPage)
    }

//    MoveToAcceptanceTest
    scenario("Survey submitted with no radio button selections, but still shows confirmation page") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(500)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      When("I fill the form in without selecting radio button options")
      setAdditionalComment("Blah blooh blah la dee daaaaa")

      And("Submit the form")
      clickSubmitButton()

      Then("The data should get sent to 'audit land' - though this is questionable behaviour from the code")
      shouldSendSurveyToDatastream

      val loggedRequests = getDatastreamSubmissionsForSurvey
      val resultJsonString = loggedRequests.get(0).getBodyAsString
      val resultJson = Json.parse(resultJsonString)

      (resultJson \ "detail" \ "helpful").as[String] shouldBe "0"
      (resultJson \ "detail" \ "speed").as[String] shouldBe "0"
      (resultJson \ "detail" \ "ticketId").as[String] shouldBe "HMRC-Z2V6DUK5"
      (resultJson \ "detail" \ "serviceId").as[String] shouldBe "arbitrary service id"
      (resultJson \ "detail" \ "improve").as[String] shouldBe "Blah blooh blah la dee daaaaa"

      And("I should see the failure page, but i cant determine a failure has occurred, so i show the conf page. lovely.")
      on(SurveyConfirmationPage)
    }

//    MoveToAcceptanceTest
    scenario("Survey form errors, but still shows confirmation page again") {

      Given("I go to the survey form page")
        goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5","arbitrary%20service%20id"))

      When("I successfully fill in the form")
        selectHowHelpfulTheResponseWas("strongly-agree")
        selectHowSpeedyTheResponseWas("strongly-agree")
        setAdditionalComment("Blah blooh blah la dee daaaaa")

      And("Submit the form")
        clickSubmitButton()

      Then("The data should get sent to 'audit land' - though this is questionable behaviour from the code")
      shouldSendSurveyToDatastream

      And("I should see the failure page, but i cant determine a failure has occurred, so i show the conf page. lovely.")
        on(SurveyConfirmationPage)
    }
  }

  def shouldSendSurveyToDatastream = {
    eventually {
      getDatastreamSubmissionsForSurvey.size() shouldBe 1
    }
  }

  def shouldNotSendSurveyToDatastream = {
    eventually {
      getDatastreamSubmissionsForSurvey.size() shouldBe 0
    }
  }

  def getDatastreamSubmissionsForSurvey = WireMock.findAll(postRequestedFor(urlEqualTo("/write/audit"))
    .withRequestBody(matching(".*DeskproSurvey.*")))
}

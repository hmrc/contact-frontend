package features

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.i18n.Messages
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeApplication
import support.StubbedFeatureSpec
import support.page.SurveyPage._
import support.page.{SurveyConfirmationPage, SurveyConfirmationPageWelsh, SurveyPageWithTicketAndServiceIds}
import uk.gov.hmrc.play.test.WithFakeApplication

class LanguageSwitchingFeature extends StubbedFeatureSpec with WithFakeApplication {

  override lazy val app = new FakeApplication(
    additionalConfiguration = Map("application.langs" -> "en,cy",
      "govuk-tax.Test.enableLanguageSwitching" -> true)
  )

  feature("Language Switching") {

    scenario("Switch from English to Welsh in the survey") {

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds("HMRC-Z2V6DUK5", "arbitrary%20service%20id"))

      And("I click on the switch language link")
      click on linkText("Cymraeg")

      i_see(Messages("WELSH-Survey"))
      i_see(Messages("WELSH-How satisfied are you with the answer we gave you?"))
      i_see(Messages("WELSH-How satisfied are you with the speed of our reply?"))
      i_see(Messages("WELSH-Tell us how we can improve the support we give you."))
      i_see(Messages("WELSH-2500 characters or less"))
      i_see(Messages("WELSH-Is there anything wrong with this page?"))
      i_see(Messages("English"))

      And("I click on the switch language link")
      click on linkText("English")

      i_see(Messages("Survey"))
      i_see(Messages("How satisfied are you with the answer we gave you?"))
      i_see(Messages("How satisfied are you with the speed of our reply?"))
      i_see(Messages("Tell us how we can improve the support we give you."))
      i_see(Messages("2500 characters or less"))
      i_see(Messages("Is there anything wrong with this page?"))
      i_see(Messages("Cymraeg"))
    }

    scenario("Show confirmation message in Welsh after submitting the form in Welsh") {

      val invalidTicketId = "HMRC-Z2V6DUK5"

      WireMock.stubFor(post(urlEqualTo("/write/audit")).willReturn(aResponse().withStatus(200)))

      Given("I go to the survey form page")
      goOn(new SurveyPageWithTicketAndServiceIds(invalidTicketId,"arbitrary%20service%20id"))

      And("I click on the switch language link")
      click on linkText("Cymraeg")

      When("I successfully fill in the form")
      selectHowHelpfulTheResponseWas("strongly-agree")
      selectHowSpeedyTheResponseWas("strongly-agree")
      setAdditionalComment("Rhoedd eich mam yn fochdew a'ch tad yn aroglu o eirin ysgaw!")

      And("Submit the form")
      clickSubmitButton()

      And("I get to the Welsh Language confirmation page")
      on(SurveyConfirmationPageWelsh)
    }

  }

}


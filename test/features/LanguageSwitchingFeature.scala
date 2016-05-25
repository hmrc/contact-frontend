package features

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.i18n.Messages
import support.StubbedFeatureSpec
import support.page.{SurveyPageWithTicketAndServiceIds, SurveyConfirmationPage}

class LanguageSwitchingFeature extends StubbedFeatureSpec {

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
      i_see(Messages("WELSH-Get help with this page."))
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

  }
}

package test

import org.scalatest.GivenWhenThen
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsNull
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._

class GetHelpWithThisPageFeatureISpec extends UnitSpec with GuiceOneAppPerSuite with GivenWhenThen {
  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "features.getHelpWithThisPage.split" -> "50"
    )
    .build()


  "GetHelpWithThisPageFeature" should {
    "Display feature A" in {
      checkFeatureForText("AAAAAA", """Is there anything wrong with this page\?""")

    }

    "Display feature B" in {
      checkFeatureForText("BBBBBB", "Tell us about a problem with the service")
    }
  }

  private def checkFeatureForText(deviceID: String, text: String): Unit = {
    val request = FakeRequest(Helpers.GET,
      "/contact/beta-feedback-unauthenticated",
      FakeHeaders(Seq("deviceID" -> deviceID)),
      JsNull)

    val response = route(app, request).get

    status(response) shouldBe OK

    contentAsString(response) should include regex s"""<[^>]*id=[^>]*get-help-action[^>]*>${text}</[^>]*>"""
  }
}

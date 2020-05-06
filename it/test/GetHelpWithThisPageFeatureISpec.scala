package test

import javax.inject.Inject
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.DefaultHttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsNull
import play.api.mvc.Cookie
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import play.filters.cors.CORSFilter
import uk.gov.hmrc.http.CookieNames
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters
import uk.gov.hmrc.play.bootstrap.filters.frontend.deviceid.DeviceIdFilter

class FeatureTestContactFrontendFilters @Inject()(
    defaultFilters: FrontendFilters,
    playCORSFilter: CORSFilter)
    extends DefaultHttpFilters(
      (defaultFilters.filters :+ playCORSFilter)
        .filterNot(_.isInstanceOf[DeviceIdFilter]): _*)

class GetHelpWithThisPageFeatureISpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with GivenWhenThen {
  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "features" -> List(
        "feature=GetHelpWithThisPageNewWordingOfEntryLink;bucketFrom=0;bucketTo=50")
    )
    .configure("play.http.filters" -> "test.FeatureTestContactFrontendFilters")
    .build()


  "GetHelpWithThisPageFeature" should {
    "Display version with new wording of the entry link if within experiment" in {
      checkFeatureForText( "BBBBBB",
                          """Is this page not working properly\?""")

    }

    "Display the old version if within experiment" in {
      checkFeatureForText("AAAAAA", "Tell us about a problem with the service")
    }
  }

  private def checkFeatureForText(deviceID: String, text: String): Unit = {
    val request = FakeRequest(Helpers.GET,
                              "/contact/beta-feedback-unauthenticated",
                              FakeHeaders(Seq.empty),
                              JsNull)
      .withCookies(Cookie(CookieNames.deviceID, deviceID))

    val response = route(app, request).get

    status(response) shouldBe OK

    contentAsString(response) should include regex s"""<[^>]*id=[^>]*get-help-action[^>]*>${text}</[^>]*>"""
  }
}

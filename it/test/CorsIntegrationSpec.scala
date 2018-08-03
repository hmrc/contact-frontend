package test

import org.scalatest.GivenWhenThen
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames.{HOST, ORIGIN}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsValue}
import play.api.mvc.Result
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

class CorsIntegrationSpec extends UnitSpec with GuiceOneAppPerSuite with GivenWhenThen {

  private val fakeRequest = FakeRequest(OPTIONS, "/contact/contact-hmrc",
    FakeHeaders(Seq(
      ORIGIN -> "https://ewf.companieshouse.gov.uk",
      HOST -> "localhost",
      ACCESS_CONTROL_REQUEST_HEADERS -> USER_AGENT,
      ACCESS_CONTROL_REQUEST_METHOD -> POST
    )), JsNull)


  "Play Framework CORS configuration" should {

    "respond with 200 for allowed origin" in {
      val optionsRequest = fakeRequest

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe OK

      val headers = optionsResponse.header.headers

      headers(ACCESS_CONTROL_ALLOW_HEADERS) shouldBe USER_AGENT.toLowerCase
      headers(ACCESS_CONTROL_ALLOW_METHODS) shouldBe POST
      headers(ACCESS_CONTROL_ALLOW_ORIGIN)  shouldBe "https://ewf.companieshouse.gov.uk"
    }

    "respond with 403 for a disallowed origin" in {
      val optionsRequest = fakeRequest.withHeaders(ORIGIN -> "http://bad.domain.com")

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe FORBIDDEN
    }

    "respond with 403 for a disallowed http method" in {
      val optionsRequest = fakeRequest.withHeaders(ACCESS_CONTROL_REQUEST_METHOD -> DELETE)

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe FORBIDDEN
    }

    "respond with 403 for a disallowed http header" in {
      val optionsRequest = fakeRequest.withHeaders(ACCESS_CONTROL_REQUEST_HEADERS -> "disallowed header")

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe FORBIDDEN
    }
  }
}

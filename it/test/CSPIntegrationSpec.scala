package test

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.FakeRequest

import scala.concurrent.Await
import scala.concurrent.duration._

class CSPIntegrationSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with GivenWhenThen {

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  private val fakeRequest = FakeRequest(
    GET,
    "/contact/contact-hmrc-unauthenticated?service=foo"
  )

  "Play Framework CSP configuration" should {
    "respond with the correct CSP header" in {
      val response: Result = Await.result(route(app, fakeRequest).get, 1 seconds)

      response.header.status shouldBe OK

      val headers = response.header.headers

      headers(
        CONTENT_SECURITY_POLICY
      ) shouldBe "default-src 'self' 'unsafe-inline' 'unsafe-eval' www.googletagmanager.com www.google-analytics.com cdnjs.cloudflare.com www.gstatic.com fonts.googleapis.com fonts.gstatic.com data:"
    }
  }
}

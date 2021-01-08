package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class AkkaIntegrationSpec extends AnyWordSpec with Matchers with GuiceOneServerPerSuite {
  private def baseUrl          = s"http://localhost:$port/contact/accessibility-unauthenticated?service=pay-frontend"
  private def veryLongUrl      = s"$baseUrl&referrerUrl=https%3A%2F%2Fexample.com%2F" + ("x" * 10000)
  private def extremelyLongUrl = s"$baseUrl&referrerUrl=https%3A%2F%2Fexample.com%2F" + ("x" * 20000)

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  "The Play Framework Akka configuration" should {

    "respond with 200 when given a very long valid URL" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response =
        await(wsClient.url(veryLongUrl).get())

      response.status should be(OK)
    }

    "respond with 414 when given an extremely long valid URL" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response =
        await(wsClient.url(extremelyLongUrl).get())

      response.status should be(REQUEST_URI_TOO_LONG)
    }
  }
}

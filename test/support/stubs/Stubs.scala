package support.stubs

import java.net.URLEncoder
import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, RequestPatternBuilder, UrlMatchingStrategy, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.MustMatchers
import org.skyscreamer.jsonassert.JSONCompareMode
import play.api.libs.Crypto
import play.api.test.Helpers
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, PlainText}
import uk.gov.hmrc.http.SessionKeys

trait Stubs {
  def stubFor(stub: Stub) {
    stub.create()
  }
}

trait Stub extends MustMatchers {
  lazy val instance = new WireMockServer(wireMockConfig().port(stubPort))

  def stubPort : Int

  def reset(): Unit = {
    instance.resetMappings()
  }

  def create() : Unit

  def start() : Unit = {
    instance.start()
  }

  def shutdown() : Unit = {
    instance.shutdown()
  }

  def stubForPage(urlMatchingStrategy: UrlMatchingStrategy) = {
    instance.stubFor(get(urlMatchingStrategy))
  }

  def stubForPage(urlMatchingStrategy: UrlMatchingStrategy, heading: String)(body: String = "This is a stub") = {
    instance.stubFor(get(urlMatchingStrategy)
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"<html><body><h1>$heading</h1>$body</body></html>")
      ))
  }

  def service_will_return_payload_for_get_request(url: String, delayMillis: Int = 0)(payload: String) = {
    instance.stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withFixedDelay(delayMillis)
          .withBody(payload)))
  }

  def service_will_return_payload_for_POST_request(url: String, delayMillis: Int = 0)(payload: String) = {
    instance.stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withFixedDelay(delayMillis)
          .withBody(payload)))
  }

  def service_will_fail_on_get_request(url: String, status: Int) = {
    instance.stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(status)))
  }

  def service_will_fail_on_POST_request(url: String, status: Int) = {
    instance.stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(status)))
  }


  def verify_post(to: String, body: String, compareMode: JSONCompareMode) = {
    instance.verify(postRequestedFor(urlEqualTo(to))
      .withRequestBody(equalToJson(body, compareMode)))
  }

  def verify_post_no_hit(to: String) = instance.findAll(postRequestedFor(urlEqualTo(to))) must be('isEmpty)
}


trait SessionCookieBaker {
  def cookieValue(sessionData: Map[String, String]) = {
    def encode(data: Map[String, String]): String = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      Crypto.sign(encoded, "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes) + "-" + encoded
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM("gvBoGdgzqG1AarzF1LY0zQ==", Seq()).encrypt(PlainText(encodedCookie)).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }
}

object Auditing extends Stub {

  def create() = {
    instance.stubFor(post(urlEqualTo("/write/audit"))
      .willReturn(
        aResponse()
          .withStatus(204)))

    instance.stubFor(post(urlEqualTo("/write/audit/merged"))
      .willReturn(
        aResponse()
          .withStatus(204)))
  }

  def stubFor(mappingBuilder: MappingBuilder) = instance.stubFor(mappingBuilder)

  def findAll(requestPatternBuilder: RequestPatternBuilder) = instance.findAll(requestPatternBuilder)

  override def stubPort = 11111
}


object Login extends Stub with SessionCookieBaker {

  val SessionId = s"stubbed-${UUID.randomUUID}"

  def create() = {
    stubSuccessfulLogin()
  }

  def stubSuccessfulLogin() = {
    val data = Map(
      SessionKeys.sessionId -> SessionId,
      SessionKeys.userId -> "/auth/oid/1234567890",
      SessionKeys.authToken -> "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI",
      SessionKeys.token -> "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI",
      SessionKeys.affinityGroup -> "Organisation",
      SessionKeys.lastRequestTimestamp -> new java.util.Date().getTime.toString,
      SessionKeys.name -> "JOHN THE SAINSBURY",
      SessionKeys.authProvider -> "GGW"
    )

    instance.stubFor(get(urlEqualTo(s"/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A${Helpers.testServerPort}%2Fcontact%2Fbeta-feedback&origin=contact-frontend"))
      .willReturn(aResponse()
      .withStatus(303)
      .withHeader(HeaderNames.SET_COOKIE, cookieValue(data))
      .withHeader(HeaderNames.LOCATION, s"http://localhost:${Helpers.testServerPort}/contact/beta-feedback")))

    instance.stubFor(get(urlEqualTo(s"/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A${Helpers.testServerPort}%2Fcontact%2Fcontact-hmrc&origin=contact-frontend"))
      .willReturn(aResponse()
      .withStatus(303)
      .withHeader(HeaderNames.SET_COOKIE, cookieValue(data))
      .withHeader(HeaderNames.LOCATION, s"http://localhost:${Helpers.testServerPort}/contact/contact-hmrc")))

    val body = s"""
                 |{ "allEnrolments" : []}
                 """.stripMargin

    instance.stubFor(post(urlEqualTo("/auth/authorise")).atPriority(1).withHeader(HeaderNames.AUTHORIZATION, WireMock.matching(".*"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(body)))

    instance.stubFor(post(urlEqualTo("/auth/authorise")).atPriority(2)
      .willReturn(
        aResponse()
          .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"MissingBearerToken\"")
          .withBody("{}")))

  }

  override def stubPort = 11112
}

object Deskpro extends Stub {
  override def create() = {

    instance.stubFor(post(urlEqualTo("/deskpro/get-help-ticket"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody("""{"ticket_id": 1}""")
      ))

    instance.stubFor(post(urlEqualTo("/deskpro/feedback"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody("""{"ticket_id": 10}""")
      ))
  }

  override def stubPort = 11113
}

object ExternalPages extends Stub {
  override def create() = {
    stubForPage(urlEqualTo("/external/page"), "Page with links") {
      s"""<a href="http://localhost:${Helpers.testServerPort}/contact/beta-feedback-unauthenticated">Leave feedback</a>
        |<a href="http://localhost:${Helpers.testServerPort}/contact/contact-hmrc">Contact HMRC</a>
      """.stripMargin
    }
  }

  override def stubPort = 11115
}

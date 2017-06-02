package support.stubs

import java.net.URLEncoder
import java.util.UUID

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.Crypto
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, PlainText}
import uk.gov.hmrc.play.http.SessionKeys

trait Stubs {
  def stubFor(stub: Stub) {
    stub.create()
  }
}

trait Stub {
  def create(): Unit

  def stubForPage(urlMatchingStrategy: UrlMatchingStrategy, heading: String)(body: String = "This is a stub") = {
    stubFor(get(urlMatchingStrategy)
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"<html><body><h1>$heading</h1>$body</body></html>")
      ))
  }
}

trait SessionCookieBaker {
  def cookieValue(sessionData: Map[String,String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(Crypto.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM("gvBoGdgzqG1AarzF1LY0zQ==", Seq()).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }
}

object Auditing extends Stub {

  def create() = {
    stubFor(post(urlEqualTo("/write/audit"))
      .willReturn(
        aResponse()
          .withStatus(200)))

    stubFor(post(urlEqualTo("/write/audit/merged"))
      .willReturn(
        aResponse()
          .withStatus(200)))
  }
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
      SessionKeys.name -> "JOHN THE SAINSBURY",
      SessionKeys.token -> "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI",
      SessionKeys.affinityGroup -> "Organisation",
      SessionKeys.authProvider -> "GGW"
    )

    stubFor(get(urlEqualTo("/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9000%2Fcontact%2Fbeta-feedback&origin=contact-frontend"))
      .willReturn(aResponse()
      .withStatus(303)
      .withHeader(HeaderNames.SET_COOKIE, cookieValue(data))
      .withHeader(HeaderNames.LOCATION, "http://localhost:9000/contact/beta-feedback")))

    stubFor(get(urlEqualTo("/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9000%2Fcontact%2Fcontact-hmrc&origin=contact-frontend"))
      .willReturn(aResponse()
      .withStatus(303)
      .withHeader(HeaderNames.SET_COOKIE, cookieValue(data))
      .withHeader(HeaderNames.LOCATION, "http://localhost:9000/contact/contact-hmrc")))

    stubFor(get(urlEqualTo("/auth/authority"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
             |{
              |    "uri": "/auth/oid/1234567890",
              |    "loggedInAt": "2014-06-09T14:57:09.522Z",
              |    "previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
              |    "accounts": {
              |    },
              |    "levelOfAssurance": "2",
              |    "credentialStrength": "weak",
              |    "confidenceLevel" : 50
              |}
              |
            """.stripMargin
          )))
  }

}

object Deskpro extends Stub {
  override def create() = {

    stubFor(post(urlEqualTo("/deskpro/ticket"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody("""{"ticket_id": 1}""")
      ))

    stubFor(post(urlEqualTo("/deskpro/feedback"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody("""{"ticket_id": 10}""")
      ))
  }
}

object ExternalPages extends Stub {
  override def create() = {
    stubForPage(urlEqualTo("/external/page"), "Page with links") {
      """<a href="http://localhost:9000/contact/beta-feedback-unauthenticated">Leave feedback</a>
        |<a href="http://localhost:9000/contact/contact-hmrc">Contact HMRC</a>
      """.stripMargin
    }
  }
}

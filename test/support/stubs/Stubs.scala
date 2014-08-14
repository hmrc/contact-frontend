package support.stubs

import java.net.URLEncoder
import java.util.UUID

import com.github.tomakehurst.wiremock.client.{WireMock, UrlMatchingStrategy}
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.Crypto
import play.mvc.Http.HeaderNames
import support.steps.Env
import uk.gov.hmrc.crypto.CompositeSymmetricCrypto
import uk.gov.hmrc.play.http.SessionKeys


trait Stubs {
  WireMock.configureFor(Env.stubHost, Env.stubPort)

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
    def encode(data: Map[String, String]): String = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      Crypto.sign(encoded, "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes) + "-" + encoded
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aes("gvBoGdgzqG1AarzF1LY0zQ==", Seq()).encrypt(encodedCookie)

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

  def create() = {
    stubSuccessfulLogin()    
  }

  def stubSuccessfulLogin() = {
    val data = Map(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
      SessionKeys.userId -> "/auth/oid/1234567890",
      SessionKeys.authToken -> "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI",
      SessionKeys.name -> "JOHN THE SAINSBURY",
      SessionKeys.token -> "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI",
      SessionKeys.affinityGroup -> "Organisation",
      SessionKeys.authProvider -> "GGW"
    )


    stubFor(get(urlEqualTo("/sign-in?continue=/beta-feedback"))
      .willReturn(aResponse()
      .withStatus(303)
      .withHeader(HeaderNames.SET_COOKIE, cookieValue(data))
      .withHeader(HeaderNames.LOCATION, "http://localhost:9000/beta-feedback")))


    stubFor(post(urlEqualTo("/login"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            """
              |{
              |    "authId": "/auth/oid/1234567890",
              |    "credId": "cred-id-12345",
              |    "name": "JOHN THE SAINSBURY",
              |    "affinityGroup": "Organisation",
              |    "encodedGovernmentGatewayToken": "PGdhdGV3YXk6R2F0ZXdheVRva2VuIHhtbG5zOndzdD0iaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNC8wNC90cnVzdCIgeG1sbnM6d3NhPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA0LzAzL2FkZHJlc3NpbmciIHhtbG5zOndzc2U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3Mtd3NzZWN1cml0eS1zZWNleHQtMS4wLnhzZCIgeG1sbnM6d3N1PSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktdXRpbGl0eS0xLjAueHNkIiB4bWxuczpzb2FwPSJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy9zb2FwL2VudmVsb3BlLyI+PGdhdGV3YXk6Q3JlYXRlZD4yMDE0LTA2LTA5VDA5OjM5OjA2WjwvZ2F0ZXdheTpDcmVhdGVkPjxnYXRld2F5OkV4cGlyZXM+MjAxNC0wNi0wOVQxMzozOTowNlo8L2dhdGV3YXk6RXhwaXJlcz48Z2F0ZXdheTpVc2FnZT5TdGFuZGFyZDwvZ2F0ZXdheTpVc2FnZT48Z2F0ZXdheTpPcGFxdWU+ZXlKamNtVmtTV1FpT2lKamNtVmtMV2xrTFRVME16SXhNak13TURBeE9TSXNJbU55WldGMGFXOXVWR2x0WlNJNklqSXdNVFF0TURZdE1EbFVNRGs2TXprNk1EWXVNREF3V2lJc0ltVjRjR2x5ZVZScGJXVWlPaUl5TURFMExUQTJMVEE1VkRFek9qTTVPakEyTGpBd01Gb2lmUT09PC9nYXRld2F5Ok9wYXF1ZT48L2dhdGV3YXk6R2F0ZXdheVRva2VuPg=="
              |}|
            """.stripMargin
          )))


    stubFor(post(urlEqualTo("/auth/cred-id/cred-id-12345/exchange"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
              |{
              |    "authToken": {
              |        "authToken": "Bearer JV5QMvw5jffiTpVBWigC0u//y0NAgJwSEO/jDALEotc="
              |    },
              |    "authority": {
              |        "uri": "/auth/oid/1234567890",
              |        "loggedInAt": "2014-06-09T14:57:09.522Z",
              |        "previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
              |        "credentials": {
              |            "gatewayId": "cred-id-12345",
              |            "idaPids": []
              |        },
              |        "accounts": {
              |        }
              |    }
              |}
            """.stripMargin
          )))

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
              |    "credentials": {
              |        "gatewayId": "cred-id-12345",
              |        "idaPids": []
              |    },
              |    "accounts": {
              |    }
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
    stubForPage(urlEqualTo("/external/page-to-feedback"), "Page with feedback") {
      """<a href="http://localhost:9000/beta-feedback-unauthenticated">Link to feedback</a>"""
    }
  }
}

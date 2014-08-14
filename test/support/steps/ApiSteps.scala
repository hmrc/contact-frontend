package support.steps

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.skyscreamer.jsonassert.JSONCompareMode

trait ApiSteps extends BaseSteps {

    def service_will_return_payload_for_get_request(url: String, delayMillis: Int = 0)(payload: String) = {
      stubFor(get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withFixedDelay(delayMillis)
            .withBody(payload)))
    }

  def service_will_return_payload_for_POST_request(url: String, delayMillis: Int = 0)(payload: String) = {
    stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withFixedDelay(delayMillis)
          .withBody(payload)))
  }

    def service_will_fail_on_get_request(url: String, status: Int) = {
      stubFor(get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)))
    }

  def service_will_fail_on_POST_request(url: String, status: Int) = {
    stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(status)))
  }


  def verify_post(to: String, body: String, compareMode: JSONCompareMode) = {
    verify(postRequestedFor(urlEqualTo(to))
      .withRequestBody(equalToJson(body, compareMode)))
  }

  def verify_post_no_hit(to: String) = WireMock.findAll(postRequestedFor(urlEqualTo(to))) should be('isEmpty)

}

package support.steps

import com.github.tomakehurst.wiremock.client.WireMock._

trait ApiSteps extends BaseSteps {

    def the_call_to_url_will_return_payload(url: String)(payload: String) = {
      stubFor(get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(payload)))
    }

    def the_call_to_url_fails_with_status(url: String, status: Int) = {
      stubFor(get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)))
    }

}

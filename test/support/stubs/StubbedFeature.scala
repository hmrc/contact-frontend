package support.stubs

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.AcceptanceSpec

trait StubbedFeature extends AcceptanceSpec with Stubs {

  Before {
    stubFor(Auditing)
    stubFor(Login)
    stubFor(Deskpro)
  }

  After {
    WireMock.reset()
  }

}

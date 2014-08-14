package support.stubs

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.AcceptanceSpec
import support.steps.Env


trait StubbedFeature extends AcceptanceSpec with Stubs {

  Before {
    Common.before()
  }

  After {
    Common.after()
  }

}


trait NoJsFeature extends AcceptanceSpec with Stubs {

  Before {
    Env.disableJavascript()

    Common.before()
  }

  After {
    Common.after()

    Env.enableJavascript()
  }

}


object Common extends Stubs {
  def before() = {
    stubFor(Auditing)
    stubFor(Login)
    stubFor(Deskpro)
    stubFor(ExternalPages)
  }

  def after() = {
    WireMock.reset()
  }
}

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

  protected override def beforeAll() {
    super.beforeAll()

    Env.enableJavascript()
  }

}


trait NoJsFeature extends AcceptanceSpec with Stubs {

  Before {
    Common.before()
  }

  After {
    Common.after()
  }

  protected override def beforeAll() {
    super.beforeAll()

    Env.disableJavascript()
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

package support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest._
import support.behaviour.NavigationSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.TestServer
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs._
import support.util.Env

trait StubbedFeatureSpec
  extends FeatureSpec
    with GivenWhenThen
//    with OneServerPerSuite
    with Stubs
    with BeforeAndAfter
    with BeforeAndAfterEach
    with org.scalatest.Background
    with BeforeAndAfterAll
    with NavigationSugar
    with NavigationSteps
    with ApiSteps
    with ObservationSteps
    with OptionValues {

  lazy val port = 9000
  val stubPort = 11111
  val stubHost = "localhost"
  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  lazy val app: Application = new GuiceApplicationBuilder().build()
  var testServer = TestServer(port, app)

  override def beforeAll() = {
    Env.useJavascriptDriver()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
    testServer.start()
  }

  override def afterAll() = {
    testServer.stop()
    wireMockServer.stop()
    Env.deleteAllCookies()
  }

  override def beforeEach() = {
    Env.deleteCookies()
    WireMock.reset()
    stubFor(Auditing)
    stubFor(Login)
    stubFor(Deskpro)
    stubFor(ExternalPages)
  }
}

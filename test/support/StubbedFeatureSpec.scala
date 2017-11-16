package support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite
import support.behaviour.NavigationSugar
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs._
import support.util.Env

trait StubbedFeatureSpec
  extends FeatureSpec
  with GivenWhenThen
  with ShouldMatchers
  with OneServerPerSuite
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

  override lazy val port = 9000

  val stubPort = 11111
  val stubHost = "localhost"
  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  override def beforeAll() = {
    Env.useJavascriptDriver()
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  override def afterAll() = {
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

package support

import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite
import play.api.Logger
import support.behaviour.NavigationSugar
import support.steps.{ApiSteps, NavigationSteps, ObservationSteps}
import support.stubs._
import support.util.Env

trait StubbedFeatureSpec
  extends FeatureSpec
    with GivenWhenThen
    with Stubs
    with BeforeAndAfter
    with BeforeAndAfterEach
    with org.scalatest.Background
    with BeforeAndAfterAll
    with NavigationSugar
    with NavigationSteps
    with ApiSteps
    with ObservationSteps
    with OptionValues
    with OneServerPerSuite {

   val logger = Logger("tests")

  override def beforeAll() = {
    Env.useJavascriptDriver()
    Auditing.start()
    Login.start()
    Deskpro.start()
    ExternalPages.start()
  }

  override def afterAll() = {
    Auditing.shutdown()
    Login.shutdown()
    Deskpro.shutdown()
    ExternalPages.shutdown()
    Env.deleteAllCookies()
  }

  override def beforeEach() = {
    Env.deleteCookies()

    Auditing.reset()
    Login.reset()
    Deskpro.reset()
    ExternalPages.reset()

    Auditing.create()
    Login.create()
    Deskpro.create()
    ExternalPages.create()
  }
}

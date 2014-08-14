package support.steps

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import uk.gov.hmrc.integration.framework.SingletonDriver

import scala.util.Try


object Env {
  var host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://web-qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse("http://localhost:9000")
  }


  val stubPort = 11111

  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  wireMockServer.start()
  var driver: WebDriver = SingletonDriver.getInstance()

  def enableJavascript() =  {
    SingletonDriver.setJavascript(true)
    driver = SingletonDriver.getInstance()
  }

  def disableJavascript() = {
    SingletonDriver.setJavascript(false)
    driver = SingletonDriver.getInstance()
  }


  def addShutdownHook(body: => Unit) =
    Runtime.getRuntime addShutdownHook new Thread { override def run { body } }
  addShutdownHook {
    Try(driver.quit())
    wireMockServer.stop()
  }


}

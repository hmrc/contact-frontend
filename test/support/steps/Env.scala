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


  val dc = DesiredCapabilities.firefox()

  private val proxyPort: String = System.getProperty("http.proxyPort")
  if (proxyPort != null) {
    val proxy = new org.openqa.selenium.Proxy()
    proxy.setHttpProxy(s"localhost:$proxyPort")
    dc.setCapability(CapabilityType.PROXY, proxy)
  }

  val driver: WebDriver = SingletonDriver.getInstance()


  def addShutdownHook(body: => Unit) =
    Runtime.getRuntime addShutdownHook new Thread { override def run { body } }

  addShutdownHook {
    Try(driver.quit())
    wireMockServer.stop()
  }


}

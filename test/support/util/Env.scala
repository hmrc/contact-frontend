package support.util

import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test.Helpers
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {

  lazy val host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://www.qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse(s"http://localhost:${Helpers.testServerPort}")
  }

  val stagingDeskproBaseUrl = "https://deskpro.tools.staging.tax.service.gov.uk"
  val stagingDeskproAgentUrl = s"${stagingDeskproBaseUrl}/agent"

  val hMRCDeskproBaseUrl = "https://hmrc-deskpro.public.mdtp/deskpro"

  var driverHolder : DriverWrapper = JsDriverWrapper

  def driver = driverHolder.getInstance

  def useJavascriptDriver() = switchDriver(JsDriverWrapper)

  def useNonJavascriptDriver() = switchDriver(NoJsDriverWrapper)

  def switchDriver(newHolder : DriverWrapper): Unit = {
    if (driverHolder != newHolder) {
      driverHolder.stop()
    }
    driverHolder = newHolder
  }

  def deleteCookies() = {
    driver.manage().deleteAllCookies()
  }

  sys addShutdownHook {
    SingletonDriver.closeInstance()
    JsDriverWrapper.stop()
  }
}

trait DriverWrapper {
  def getInstance : WebDriver
  def stop() : Unit
}

object JsDriverWrapper extends DriverWrapper {
  override def getInstance: WebDriver = SingletonDriver.getInstance()

  override def stop(): Unit = SingletonDriver.closeInstance()
}

object NoJsDriverWrapper extends DriverWrapper {
  private var instanceOption: Option[HtmlUnitDriver] = None

  def getInstance() = {
    instanceOption getOrElse initialiseBrowser()
  }

  def initialiseBrowser() = {
    val instance = new HtmlUnitDriver(false)
    instanceOption = Some(instance)
    instance
  }

  def stop() = {
    instanceOption.foreach { instance =>
      instance.close()
      instance.quit()
      instanceOption = None
    }
  }

}

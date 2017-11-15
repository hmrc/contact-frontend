package support.util

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {
  var host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://www.qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse("http://localhost:9000")
  }

  val stagingDeskproBaseUrl = "https://deskpro.tools.staging.tax.service.gov.uk"
  val stagingDeskproAgentUrl = s"${stagingDeskproBaseUrl}/agent"

  val hMRCDeskproBaseUrl = "https://hmrc-deskpro.public.mdtp/deskpro"


  private var jsEnabled = true

  def jsDriver = SingletonDriver.getInstance()
  def htmlUnitDriver = NoJsDriver.getInstance()

  def driver = jsEnabled match {
    case true => jsDriver
    case false => htmlUnitDriver
  }

  def useJavascriptDriver() = {
    jsEnabled = true
  }

  def useNonJavascriptDriver() = {
    SingletonDriver.closeInstance()
    jsEnabled = false
  }

  def deleteCookies() = {
    driver.manage().deleteAllCookies()
  }

  def deleteAllCookies() = {
    jsDriver.manage().deleteAllCookies()
    htmlUnitDriver.manage().deleteAllCookies()
  }

  sys addShutdownHook {
    SingletonDriver.closeInstance()
    NoJsDriver.closeInstance()
  }
}

object NoJsDriver {
  private var instanceOption: Option[HtmlUnitDriver] = None

  def getInstance() = {
    instanceOption getOrElse initialiseBrowser()
  }

  def initialiseBrowser() = {
    val instance = new HtmlUnitDriver(false)
    instanceOption = Some(instance)
    instance
  }

  def closeInstance() = {
    instanceOption.foreach { instance =>
      instance.close()
      instance.quit()
    }
  }

}

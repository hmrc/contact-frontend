package support.util

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.DesiredCapabilities


object Env {
  var host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://www.qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse("http://localhost:9000")
  }

  val stagingDeskproBaseUrl = "https://deskpro.tools.staging.tax.service.gov.uk"
  val stagingDeskproAgentUrl = s"${stagingDeskproBaseUrl}/agent"

  val hMRCDeskproBaseUrl = "https://hmrc-deskpro.public.mdtp/deskpro"


  private var jsEnabled = true

  lazy val systemProperties = System.getProperties

  def getDriver(): WebDriver = {
    val capabilities = DesiredCapabilities.firefox()
    capabilities.setJavascriptEnabled(javascriptEnabled)


    val options = new FirefoxOptions(capabilities)

    System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true"
    )
    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "true"
    )

    new FirefoxDriver(options)
  }

  def javascriptEnabled: Boolean = {
    val jsEnabled: String = systemProperties.getProperty("javascriptEnabled")
    if(jsEnabled == null) systemProperties.setProperty("javascriptEnabled", "true")
    if (jsEnabled != "false") {
      true
    } else {
      false
    }
  }

//  def jsDriver = SingletonDriver.getInstance()
  def jsDriver = Driver.getInstance()
  def htmlUnitDriver = NoJsDriver.getInstance()

  def driver = jsEnabled match {
    case true => jsDriver
    case false => htmlUnitDriver

  }

  def useJavascriptDriver() = {
    jsEnabled = true
  }

  def useNonJavascriptDriver() = {
//    SingletonDriver.closeInstance()
    Driver.closeInstance()
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
//    SingletonDriver.closeInstance()
    Driver.closeInstance()
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

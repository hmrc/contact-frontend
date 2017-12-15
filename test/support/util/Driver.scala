package support.util

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.remote.DesiredCapabilities


object Driver {

  private var instanceOption: Option[WebDriver] = None
  private var baseWindowHandleOption : Option[String] = None
  lazy val systemProperties = System.getProperties

  def getInstance(): WebDriver = {
    instanceOption getOrElse initialiseBrowser()
  }

  def initialiseBrowser(): WebDriver = {
    val capabilities = DesiredCapabilities.firefox()
    capabilities.setJavascriptEnabled(javascriptEnabled)


    val options = new FirefoxOptions(capabilities)

    System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true"
    )
    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "true"
    )
    System.setProperty("webdriver.gecko.driver","/usr/local/bin/geckodriver")
    val instance = new FirefoxDriver(options)
    instanceOption = Some(instance)
    baseWindowHandleOption = Some(instance.getWindowHandle)
    instance
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

  def closeInstance() = {
    instanceOption foreach { instance =>
      instance.quit()
      instanceOption = None
      baseWindowHandleOption = None
    }
  }




}

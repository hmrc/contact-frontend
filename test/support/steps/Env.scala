package support.steps

import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {
  var host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://web-qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse("http://localhost:9000")
  }

  var driver: WebDriver = {
    getChromeDriver
  }

  def getChromeDriver: WebDriver = {
    val os = System.getProperty("os.name").toLowerCase.replaceAll(" ", "")
    val chromeDriver = getClass.getResource("/chromedriver/chromedriver_" + os).getPath
    Runtime.getRuntime.exec("chmod u+x " + chromeDriver)
    System.setProperty("webdriver.chrome.driver", chromeDriver)
    System.setProperty("browser", "chrome")
    SingletonDriver.getInstance()
  }

  def useJavascriptDriver() =  {
    driver = getChromeDriver
  }

  def useNoJsDriver() = {
    driver = new HtmlUnitDriver(false)
  }

  sys addShutdownHook {
    driver.quit()
  }
}

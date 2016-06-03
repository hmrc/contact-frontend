package support.steps

import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {
  var host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://www-qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse("http://localhost:9000")
  }

  var driver: WebDriver = {
    getChromeDriver
  }

  def getChromeDriver: WebDriver = SingletonDriver.getInstance()

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

package support.steps

import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {
  var host: String = System.getProperty("environment", "dev") match {
    case "qa" => "https://www-qa.tax.service.gov.uk"
    case _ => System.getProperty("host", "http://localhost:9000")
  }

  // Yuk. Rather than a var maybe we could just pull in different drivers implicitly or something?
  // The issue with the var is that it's not obvious what the lifecycle of the webdriver is
  // i.e. where and when they get set in the 'before' 'after' setups.
  var driver: WebDriver = getChromeDriver

  def getChromeDriver: WebDriver = SingletonDriver.getInstance()

  def useJavascriptDriver(): Unit =
    driver = getChromeDriver

  def useNoJsDriver(): Unit =
    driver = new HtmlUnitDriver(false)

  sys addShutdownHook {
    driver.quit()
  }
}

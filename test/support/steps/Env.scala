package support.steps

import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {
  val host: String = System.getProperty("environment", "dev") match {
    case "qa" => "https://www-qa.tax.service.gov.uk"
    case _ => System.getProperty("host", "http://localhost:9000")
  }

  private val driver: WebDriver = getDriver(true)

  def getDriver(jsEnabled: Boolean): WebDriver =
    if(jsEnabled) SingletonDriver.getInstance()
    else new HtmlUnitDriver(false)

  def getDriverWithJS: WebDriver = getDriver(true)
  def getDriverNoJS: WebDriver = getDriver(false)

  sys addShutdownHook {
    driver.quit()
  }
}

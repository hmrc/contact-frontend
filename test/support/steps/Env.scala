package support.steps

import org.openqa.selenium.WebDriver
import uk.gov.hmrc.integration.framework.SingletonDriver


object Env {
  var host = Option(System.getProperty("environment")) match {
    case Some("qa") => "https://web-qa.tax.service.gov.uk"
    case _ => Option(System.getProperty("host")).getOrElse("http://localhost:9000")
  }

  var driver: WebDriver = SingletonDriver.getInstance()

  def enableJavascript() =  {
    SingletonDriver.setJavascript(true)
    driver = SingletonDriver.getInstance()
  }

  def disableJavascript() = {
    SingletonDriver.setJavascript(false)
    driver = SingletonDriver.getInstance()
  }

}

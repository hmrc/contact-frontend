package support.steps

import support.matchers.CustomMatchers
import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.WebBrowser
import org.scalatest.{Matchers, OptionValues}


trait BaseSteps extends WebBrowser with Matchers with Eventually with OptionValues with CustomMatchers {

  implicit val webDriver: WebDriver = Env.driver

}

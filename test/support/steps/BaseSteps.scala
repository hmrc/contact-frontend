package support.steps

import org.openqa.selenium.WebDriver
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.WebBrowser
import org.scalatest.{Matchers, OptionValues}
import support.behaviour.NavigationSugar
import support.matchers.CustomMatchers


trait BaseSteps extends Matchers with OptionValues with CustomMatchers with NavigationSugar {

}

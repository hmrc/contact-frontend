package support.page

import org.openqa.selenium.{By, WebElement, WebDriver}
import org.openqa.selenium.support.ui.{ExpectedCondition, WebDriverWait}
import org.scalatest._
import org.scalatest.selenium.{WebBrowser, Page}
import support.steps.Env

trait WebPage extends Page with WebBrowser with ShouldMatchers {

  implicit def webDriver: WebDriver = Env.driver

  def isCurrentPage: Boolean

  def heading = tagName("h1").element.text

  def bodyText = tagName("body").element.text


}
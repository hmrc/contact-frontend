package support.page

import org.openqa.selenium.{By, WebDriver, WebElement}
import org.openqa.selenium.support.ui.{ExpectedCondition, WebDriverWait}
import org.scalatest._
import org.scalatest.selenium.{Page, WebBrowser}
import support.util.Env

trait WebPage extends Page with WebBrowser with MustMatchers {

  implicit def webDriver: WebDriver = Env.driver

  def isCurrentPage: Boolean

  def heading = tagName("h1").element.text

  def bodyText = tagName("body").element.text


}
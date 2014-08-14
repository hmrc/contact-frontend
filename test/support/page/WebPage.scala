package support.page

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}
import support.steps.Env

trait WebPage extends Page with WebBrowser {
  implicit def webDriver: WebDriver = Env.driver

  def isCurrentPage: Boolean = false
  def heading = tagName("h1").element.text
}

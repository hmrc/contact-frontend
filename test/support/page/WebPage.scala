package support.page

import org.openqa.selenium.WebDriver
import org.scalatest._
import org.scalatest.selenium.{Page, WebBrowser}
import support.steps.Env

trait WebPage extends Page with WebBrowser with Matchers {

  implicit def webDriver: WebDriver = Env.driver

  def title: String

  def isCurrentPage: Boolean

  def heading: String = tagName("h1").element.text

  def bodyText: String = tagName("body").element.text


}
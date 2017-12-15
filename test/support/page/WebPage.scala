package support.page

import org.openqa.selenium.{By, WebDriver}
import org.scalatest._
import org.scalatest.selenium.{Page, WebBrowser}
import support.util.Env

trait WebPage extends Page with WebBrowser with MustMatchers {

  protected val maxWaitTimeInSecs = 20

  implicit def webDriver: WebDriver = Env.driver

  def isCurrentPage: Boolean

  def heading =  webDriver.findElement(By.tagName("h1")).getText

  def bodyText = webDriver.findElement(By.tagName("body")).getText


}
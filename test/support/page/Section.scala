package support.page

import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatest.selenium.WebBrowser
import support.util.Env

import scala.util.Try

trait Section extends WebBrowser {

  implicit def webDriver: WebDriver = Env.driver

  def sectionQuery: Query

  def SectionNotDisplayedException = new NoSuchElementException("Section not displayed: " + sectionQuery)

  def section: Option[Element] = find(sectionQuery)

  def displayed = section.fold(false)(_.isDisplayed)

  override def toString: String = s"Section(${sectionQuery.toString})"

  def find(by: By): Option[WebElement] = section.flatMap(s => Try(s.underlying.findElement(by)).toOption)
}

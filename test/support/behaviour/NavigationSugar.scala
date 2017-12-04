package support.behaviour

import org.openqa.selenium.support.ui.{ExpectedCondition, WebDriverWait}
import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatest.Assertions
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.selenium.WebBrowser
import org.scalatest.selenium.WebBrowser.{go => goo}
import support.page.WebPage
import support.util.Env


trait NavigationSugar extends WebBrowser with Eventually with Assertions with IntegrationPatience {

  implicit def webDriver: WebDriver = Env.driver

  def goOn(page: WebPage) = {
    go(page)
    on(page)
  }

  def go(page: WebPage) = {
    goo to page
  }

  def on(page: WebPage) = {
    waitForPageToLoad()
    assert(page.isCurrentPage, s"Page was not loaded: $page")
  }

  def waitForPageToLoad() = {
    val wait = new WebDriverWait(webDriver, 30)
    wait.until(
      new ExpectedCondition[WebElement] {
        override def apply(d: WebDriver) = {
          d.findElement(By.tagName("body"))
        }
      }
    )
  }
}
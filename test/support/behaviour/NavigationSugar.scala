package support.behaviour

import org.openqa.selenium.support.ui.{ExpectedCondition, WebDriverWait}
import org.openqa.selenium._
import org.scalatest.Assertions
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.selenium.WebBrowser
import org.scalatest.selenium.WebBrowser.{go => goo}
import _root_.support.page.WebPage
import _root_.support.util.Env


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
    try {
      waitForPage(page)
    } catch {
      case e: TimeoutException =>
        val className = page.getClass.getSimpleName.replaceAll("\\$", "")
        throw new TimeoutException(s"Timed out waiting for page: $className\n", e)
    }
  }

  private def waitForPage(page: WebPage)(implicit webDriver: WebDriver): Unit = {
    val wait = new WebDriverWait(webDriver, 30).ignoring(classOf[StaleElementReferenceException])
    wait.until(
      new ExpectedCondition[Boolean] {
        override def apply(d: WebDriver) = page.isCurrentPage
      }
    )

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
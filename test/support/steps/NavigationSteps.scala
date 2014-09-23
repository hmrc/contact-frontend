package support.steps

import org.openqa.selenium.By.ByLinkText
import org.scalatest.time.{Span, Seconds}
import support.page.WebPage

trait NavigationSteps extends BaseSteps  {

  def i_click_on(text: String) = {
    click on linkText(text)
  }

  def i_click_on_the_link_under_section(text: String, section: String) = {
    val sectionId = section match {
      case "Footer" => "footer"
      case x => x
    }
    click on id(sectionId).webElement.findElement(new ByLinkText(text))
  }

  def i_am_on_the_home_page() = {
    shouldBeAt("home-page")
  }

  def i_am_on_the_page(heading: String) = {
    eventually(timeout(Span(10, Seconds))) {
      tagName("h1").element.text shouldBe heading
    }
  }

  def i_am_on_the_page(page: WebPage) = {
    page should be('isCurrentPage)
  }

  def another_tab_is_opened() = {
    webDriver.getWindowHandles.size() should be(2)
  }

  def switch_tab() = {
    webDriver.switchTo().window(windowHandles.last) //move this to the method that uses it in payments
  }


  def shouldBeAt(page: org.scalatest.selenium.Page) = {
    eventually {
      withClue("Was at page with title: " + tagName("h1").findElement.map(_.text) + " - ") {
        currentUrl should be(page.url)
      }
    }
  }

  def shouldBeAt(className: String) = {
    eventually {
      val body = tagName("body").element

      withClue("Was at page with title: " + tagName("h1").findElement.map(_.text) + " - ") {
        body.attribute("class").value should include(className)
      }
    }
  }
}

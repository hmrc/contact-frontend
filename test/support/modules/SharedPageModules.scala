package support.modules

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{Keys, By, WebDriver}
import org.scalatest.selenium.WebBrowser
import support.page._
import support.steps.Env

trait SharedPageModules {


  object betaBanner extends WebBrowser {

    implicit def webDriver: WebDriver = Env.driver

    def feedbackLink = linkText("feedback")

    def clickOnFeedbackLink() = {
      click on feedbackLink
      webDriver.switchTo().window(windowHandles.last)
    }
  }

  object getHelpWithThisPage extends WebBrowser {

    implicit def webDriver: WebDriver = Env.driver

    def sectionQuery = className("report-error")

    def nameField = textField("report-name")

    def emailField = textField("report-email")

    def whatWereYouDoingField = textField("report-action")

    def whatDoYouNeedHelpWithField = textField("report-error")

    def sendBtn = id("report-submit")

    def ticketId = id("ticketId").element.attribute("value")

    def problemReportHidden: Boolean = {
      val hidden = webDriver.findElements(By.xpath("//*[contains(@class, 'report-error') and contains(@class, 'hidden')]"))
      return hidden.size().equals(1)
    }

    def toggleProblemReport = {click on linkText("Get help with this page.")}

    def fillProblemReport(name: String, email: String, whatWereYouDoing: String, whatDoYouNeedHelpWith: String): Unit = {
      toggleProblemReport

      nameField.value = name
      emailField.value = email
      whatWereYouDoingField.value = whatWereYouDoing
      whatDoYouNeedHelpWithField.value = whatDoYouNeedHelpWith
    }

    def typeEmail(email: String) {
      webDriver.findElement(By.id("report-email")).sendKeys(email)
      click on nameField
    }

    def typeName(name: String) {
      webDriver.findElement(By.id("report-name")).sendKeys(name)
      click on emailField
    }

    def submitProblemReport(javascriptEnabled: Boolean = true): Unit = {
      click on sendBtn

      if (javascriptEnabled) {
        val wait = new WebDriverWait(webDriver, 15)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback-thank-you-header")))
      }
    }

    def sendProblemReport(name: String, email: String, whatWereYouDoing: String, whatDoYouNeedHelpWith: String): Unit = {
      fillProblemReport(name, email, whatWereYouDoing, whatDoYouNeedHelpWith)
      submitProblemReport()
    }
  }

}



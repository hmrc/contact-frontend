package support.modules

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}
import org.scalatest.selenium.WebBrowser
import support.page._
import support.steps.Env

trait SharedPageModules {


  object BetaBanner extends WebBrowser {

    implicit def webDriver: WebDriver = Env.driver

    def feedbackLink = linkText("feedback")

    def clickOnFeedbackLink() = {
      click on feedbackLink
      webDriver.switchTo().window(windowHandles.last)
    }
  }

  object GetHelpWithThisPage extends WebBrowser {

    implicit def webDriver: WebDriver = Env.driver

    def sectionQuery = className("report-error")

    def nameField = textField("report-name")

    def emailField = textField("report-email")

    def whatWereYouDoingField = textField("report-action")

    def whatDoYouNeedHelpWithField = textField("report-error")

    def sendBtn = id("report-submit")

    def ticketId = id("ticketId").element.attribute("value")

    def fillProblemReport(name: String, email: String, whatWereYouDoing: String, whatDoYouNeedHelpWith: String): Unit = {
      click on linkText("Get help with this page.")

      nameField.value = name
      emailField.value = email
      whatWereYouDoingField.value = whatWereYouDoing
      whatDoYouNeedHelpWithField.value = whatDoYouNeedHelpWith
    }

    def submitProblemReport(): Unit = {
      click on sendBtn

      val wait = new WebDriverWait(webDriver, 15)
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback-thank-you-header")))
    }

    def sendProblemReport(name: String, email: String, whatWereYouDoing: String, whatDoYouNeedHelpWith: String): Unit = {
      fillProblemReport(name, email, whatWereYouDoing, whatDoYouNeedHelpWith)
      submitProblemReport()
    }
  }

}



package support.modules

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver}
import org.scalatest.selenium.WebBrowser
import support.steps.Env

trait SharedPageModules {

  object betaBanner extends WebBrowser {

    implicit def webDriver: WebDriver = Env.getDriverWithJS

    def feedbackLink: LinkTextQuery = linkText("feedback")

    def clickOnFeedbackLink(): WebDriver = {
      click on feedbackLink
      webDriver.switchTo().window(windowHandles.last)
    }
  }

  object getHelpWithThisPage extends WebBrowser {

    implicit def webDriver: WebDriver = Env.getDriverWithJS

    def sectionQuery: ClassNameQuery = className("report-error")

    def nameField: TextField = textField("report-name")

    def emailInput: EmailField = emailField("report-email")

    def whatWereYouDoingField: TextField = textField("report-action")

    def whatDoYouNeedHelpWithField: TextField = textField("report-error")

    def sendBtn: IdQuery = id("report-submit")

    def ticketId: Option[String] = id("ticketId").element.attribute("value")

    def problemReportHidden: Boolean = {
      val hidden = webDriver.findElements(By.xpath("//*[contains(@class, 'report-error') and contains(@class, 'hidden')]"))
      hidden.size().equals(1)
    }

    def toggleProblemReport(): Unit = {click on linkText("Is there anything wrong with this page?")}

    def fillProblemReport(name: String, email: String, whatWereYouDoing: String, whatDoYouNeedHelpWith: String): Unit = {
      nameField.value = name
      emailInput.value = email
      whatWereYouDoingField.value = whatWereYouDoing
      whatDoYouNeedHelpWithField.value = whatDoYouNeedHelpWith
    }

    def typeEmail(email: String) {
      webDriver.findElement(By.id("report-email")).sendKeys(email)
      click on nameField
    }

    def typeName(name: String) {
      webDriver.findElement(By.id("report-name")).sendKeys(name)
      click on emailInput
    }

    def clickSubmitButton() {
      click on sendBtn
    }

    def submitProblemReport(javascriptEnabled: Boolean = true): Unit = {
      clickSubmitButton()

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



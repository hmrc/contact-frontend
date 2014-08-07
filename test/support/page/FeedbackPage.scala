package support.page

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import support.modules.SharedPageModules
import support.steps.Env

class FeedbackPage extends WebPage with SharedPageModules {
  override val url = Env.host + "/beta-feedback"

  def ratingRadioGroup = radioButtonGroup("feedback-rating")
  def nameField = textField("feedback-name")
  def emailField = textField("feedback-email")
  def commentsField = textArea("feedback-comments")
  def submitBtn = cssSelector("button[type=submit]")

  def sendFeedbackForm(rating: Int, name: String, email: String, comment: String) = {
    val wait = new WebDriverWait(webDriver, 15)
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback-rating")))

    ratingRadioGroup.value = rating.toString
    nameField.value = name
    emailField.value = email
    commentsField.value = comment

    click on submitBtn
  }
}

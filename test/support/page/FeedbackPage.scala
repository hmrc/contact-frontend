package support.page

import org.openqa.selenium.{WebDriver, By}
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import support.modules.SharedPageModules
import support.steps.Env

trait FeedbackPage extends WebPage with SharedPageModules {

  def ratingRadioGroup = radioButtonGroup("feedback-rating")
  def nameField = textField("feedback-name")
  def emailField = textField("feedback-email")
  def commentsField = textArea("feedback-comments")
  def submitBtn = cssSelector("button[type=submit]")

  def fillOutFeedbackForm(rating: Int, name: String, email: String, comment: String) = {
    val wait = new WebDriverWait(Env.driver, 15)
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback-rating")))

    ratingRadioGroup.value = rating.toString
    nameField.value = name
    emailField.value = email
    commentsField.value = comment
  }

  def submitFeedbackForm() = {
    click on submitBtn
  }
}

class UnauthenticatedFeedbackPage extends FeedbackPage {
  val url = Env.host + "/contact/beta-feedback-unauthenticated"
}

class AuthenticatedFeedbackPage extends FeedbackPage {
  val url = Env.host + "/contact/beta-feedback"
}

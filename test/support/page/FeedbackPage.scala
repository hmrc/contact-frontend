package support.page

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import support.modules.SharedPageModules
import support.util.Env

trait FeedbackPage extends WebPage with SharedPageModules {

  def ratingRadioGroup = radioButtonGroup("feedback-rating")
  def nameField = textField("feedback-name")
  def emailInput = emailField("feedback-email")
  def commentsField = textArea("feedback-comments")
  def submitBtn = cssSelector("button[type=submit]")
  def ratingsList() = {
    val eles = findAll(className("multiple-choice"))
    eles.map(_.text).toList
  }

  def serviceFieldValue(): Option[String] = find(xpath("//input[@name='service']")).flatMap(_.attribute("value"))

  def fillOutFeedbackForm(rating: Int, name: String, email: String, comment: String) = {
    val wait = new WebDriverWait(Env.driver, 3)
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback-name")))

    ratingRadioGroup.value = rating.toString
    nameField.value = name
    emailInput.value = email
    commentsField.value = comment
  }

  def submitFeedbackForm() = {
    click on submitBtn
  }
  override def isCurrentPage: Boolean = heading == "Send your feedback"
}

object UnauthenticatedFeedbackPage extends FeedbackPage {
  override val url = Env.host + "/contact/beta-feedback-unauthenticated"
}

object UnauthenticatedFeedbackPageWithServiceQueryParameter extends FeedbackPage {
  override val url = Env.host + "/contact/beta-feedback-unauthenticated?service=YTA"
}

object AuthenticatedFeedbackPage extends FeedbackPage {
  override val url = Env.host + "/contact/beta-feedback"

}


object FeedbackSuccessPage extends  WebPage with SharedPageModules {
  override def isCurrentPage: Boolean = heading=="Your feedback"

  override val url: String = Env.host + "/contact/???"
}
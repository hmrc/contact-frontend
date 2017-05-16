package support.page

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import support.modules.SharedPageModules
import support.steps.Env

trait FeedbackPage extends WebPage with SharedPageModules {

  def ratingRadioGroup: RadioButtonGroup = radioButtonGroup("feedback-rating")
  def nameField: TextField = textField("feedback-name")
  def emailInput: EmailField = emailField("feedback-email")
  def commentsField: TextArea = textArea("feedback-comments")
  def submitBtn: CssSelectorQuery = cssSelector("button[type=submit]")
  def ratingsList(): String = {
    val eles = findAll(className("label--inlineRadio--overhead"))
    var rating  = List[String]()
    for (e <- eles) rating ::= e.text.split('\n').map(_.trim.filter(_ >= ' ')).mkString
    rating.mkString(" ")
  }

  def serviceFieldValue(): Option[String] = find(xpath("//input[@name='service']")).flatMap(_.attribute("value"))

  def fillOutFeedbackForm(rating: Int, name: String, email: String, comment: String): Unit = {
    val wait = new WebDriverWait(Env.driver, 15)
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback-name")))

    ratingRadioGroup.value = rating.toString
    nameField.value = name
    emailInput.value = email
    commentsField.value = comment
  }

  def submitFeedbackForm(): Unit = {
    click on submitBtn
  }
  override def title: String = "Send your feedback"
  override def isCurrentPage: Boolean = heading == title
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
  override def title: String = "Your feedback"
  override def isCurrentPage: Boolean = heading == title

  override val url: String = Env.host + "/contact/???"
}
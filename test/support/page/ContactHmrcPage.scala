package support.page

import support.modules.SharedPageModules
import support.steps.Env

trait ContactHmrcPage extends WebPage with SharedPageModules {

  override val url: String = Env.host + "/contact/contact-hmrc"

  def nameField: TextField = textField("contact-name")
  def emailInput: EmailField = emailField("contact-email")
  def commentsField: TextArea = textArea("contact-comments")
  def submitBtn: CssSelectorQuery = cssSelector("button[type=submit]")
  def contactHmrcLink: LinkTextQuery = linkText("contact HMRC")

  def fillContactForm(name: String, email: String, comment: String): Unit = {
    nameField.value = name
    emailInput.value = email
    commentsField.value = comment
  }

  def submitContactForm(): Unit = click on submitBtn

  def sendContactForm(name: String, email: String, comment: String): Unit = {
    fillContactForm(name, email, comment)
    submitContactForm()
  }

  def clickOnContactHmrcLink(): Unit = click on contactHmrcLink

  override def title = "Help and contact"
  override def isCurrentPage: Boolean = heading == title
}

object ContactHmrcPage extends ContactHmrcPage

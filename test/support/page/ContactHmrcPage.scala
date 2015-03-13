package support.page

import support.modules.SharedPageModules
import support.steps.Env

object ContactHmrcPage extends WebPage with SharedPageModules {

  override val url = Env.host + "/contact/contact-hmrc"

  def nameField = textField("contact-name")
  def emailField = textField("contact-email")
  def emailInput = emailField("contact-comments")
  def submitBtn = cssSelector("button[type=submit]")
  def contactHmrcLink = linkText("contact HMRC")

  def fillContactForm(name: String, email: String, comment: String) = {
    nameField.value = name
    emailField.value = email
    commentsField.value = comment
  }

  def submitContactForm() = click on submitBtn

  def sendContactForm(name: String, email: String, comment: String) = {
    fillContactForm(name, email, comment)
    submitContactForm()
  }

  def clickOnContactHmrcLink() = click on contactHmrcLink

  override def isCurrentPage: Boolean = heading=="Help"
}

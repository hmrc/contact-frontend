package support.page

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}

object DeskproSignInPage extends WebPage with Eventually {
  override val url = "https://deskpro-qa.tax.service.gov.uk/agent/"

  def title: String = "Log In"
  override def isCurrentPage: Boolean = pageTitle == title

  def emailFld: DeskproSignInPage.EmailField = emailField("email")
  def passwordFld: DeskproSignInPage.PasswordField = pwdField(id("password"))
  def submitBtn: DeskproSignInPage.CssSelectorQuery = cssSelector("input[type=submit]")

  def signIn(email: String, password: String): Unit = {
    eventually(timeout(Span(10, Seconds))) {
      emailFld.value = email
      passwordFld.value = password
      click on submitBtn
    }
  }
}

class DeskproViewTicketPage(ticketId: String) extends WebPage {
  override val url: String = s"https://deskpro-qa.tax.service.gov.uk/agent/#app.tickets,inbox:all,t.o:$ticketId"

  def messageBody: ClassNameQuery = className("body-text-message")
  def profile: XPathQuery = xpath("//*[contains(@id,'profile_link')]/span")

  override def title: String = "something"
  override def isCurrentPage: Boolean = heading == title
}

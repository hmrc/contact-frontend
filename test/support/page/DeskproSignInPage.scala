package support.page

import support.util.Env

object DeskproSignInPage extends WebPage {
  override val url = Env.stagingDeskproAgentUrl

  override def isCurrentPage: Boolean = pageTitle == "Log In"

  def emailFld = emailField("email")
  def passwordFld = pwdField(id("password"))
  def submitBtn = cssSelector("input[type=submit]")

  def signIn() = {
    emailFld.value = "asrivastava@equalexperts.com"
    passwordFld.value = "Password1234"
    click on submitBtn
  }
}

class DeskproViewTicketPage(ticketId: String) extends WebPage {

  override val url: String = s"${Env.stagingDeskproAgentUrl}/#app.tickets,inbox:all,t.o:$ticketId"

  def messageBody = className("body-text-message")
  def profile = xpath("//*[contains(@id,'profile_link')]/span")

  override def isCurrentPage: Boolean = heading=="somthing"
}

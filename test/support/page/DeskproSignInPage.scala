package support.page

object DeskproSignInPage extends WebPage {
  override val url = "https://deskpro-qa.tax.service.gov.uk/index.php/agent/"

  override def isCurrentPage: Boolean = pageTitle == "Log In"

  def emailFld = emailField("email")
  def passwordFld = pwdField(id("password"))
  def submitBtn = cssSelector("input[type=submit]")

  def signIn(email: String, password: String) = {
    emailFld.value = email
    passwordFld.value = password
    click on submitBtn
  }
}

class DeskproViewTicketPage(ticketId: String) extends WebPage {
  override val url: String = s"https://deskpro-qa.tax.service.gov.uk/index.php/agent/#app.tickets,inbox:all,t.o:$ticketId"

  def messageBody = className("body-text-message")
  def profile = xpath("//*[contains(@id,'profile_link')]/span")

  override def isCurrentPage: Boolean = heading=="somthing"
}

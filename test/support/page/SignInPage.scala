package support.page

import support.steps.Env

object SignInPage extends WebPage {
  override val url: String = Env.host + "/gg/sign-in?continue=/business-account"

  override def title: String = "Sign in - Government Gateway"
  override def isCurrentPage: Boolean = pageTitle == title

  def userField: SignInPage.TextField = textField("userId")
  def passwordField: SignInPage.PasswordField = pwdField("password")
  def signInBtn: SignInPage.Element = find(id("signin")).getOrElse(throw new Exception("Missing element with id signin")) //find(className("button")).get

  def signIn(username: String, password: String): Unit = {
    userField.value = username
    passwordField.value = password
    click on signInBtn
  }
}

package support.page

import support.steps.Env

class SignInPage extends WebPage {
  override val url: String = Env.host + "/gg/sign-in?continue=/account"

  override def isCurrentPage: Boolean = find(className("button")).fold(false)(_.text == "Sign in")

  def signInBtn = find(className("button")).get

  def signIn(): Unit = {
    click on signInBtn
  }
}

object SignInLocalPage extends WebPage {
  override val url: String = Env.host + "/gg/sign-in?continue=/account"

  override def isCurrentPage: Boolean = find(className("button")).fold(false)(_.text == "Sign in")

  def userField = textField("userId")
  def passwordField = pwdField("password")
  def signInBtn = find(className("button")).get

  def signIn(username: String, password: String): Unit = {
    userField.value = username
    passwordField.value = password
    click on signInBtn
  }
}

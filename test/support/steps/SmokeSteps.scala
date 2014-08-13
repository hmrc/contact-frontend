package support.steps

import support.page.SignInLocalPage
import uk.gov.hmrc.integration.{GovernmentGatewayUsers, TestUser}

trait SmokeSteps extends BaseSteps {

  lazy val DefaultUser = GovernmentGatewayUsers.UserWithNoSARegime

  def i_sign_in(): Unit = {
    i_sign_in(DefaultUser)
  }

  def i_sign_in(user: TestUser): Unit = {
    val signInPage = new SignInLocalPage
    go to signInPage

    signInPage.signIn(user.username, user.password)
  }

}

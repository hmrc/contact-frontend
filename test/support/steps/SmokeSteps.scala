package support.steps

import support.page.SignInPage
import uk.gov.hmrc.integration.{GovernmentGatewayUsers, TestUser}

trait SmokeSteps extends BaseSteps {

  lazy val DefaultUser = GovernmentGatewayUsers.UserWithNoSARegime

  def iSignIn(): Unit = iSignIn(DefaultUser)

  def iSignIn(user: TestUser): Unit = {
    goOn(SignInPage)
    SignInPage.signIn(user.username, user.password)
  }

}

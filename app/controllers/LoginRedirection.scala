package controllers

import config.AppConfig
import play.api.Configuration
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.NoActiveSession
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.{ExecutionContext, Future}

trait LoginRedirection extends Results with RunMode {

  def appConfig: AppConfig

  def configuration: Configuration

  def loginRedirection[B <: Result](continueUrl: String)(body: Future[B])(
    implicit ec: ExecutionContext): Future[Result] =
    body.recoverWith {
      case _: NoActiveSession => Future.successful(redirectForLogin(continueUrl))
    }

  private def redirectForLogin(continueUrl: String): Result = {

    val origin: String = configuration
      .getString("sosOrigin")
      .orElse(configuration.getString("appName"))
      .getOrElse("undefined")

    lazy val companyAuthUrl = configuration.getString(s"govuk-tax.$env.company-auth.host").getOrElse("")
    val loginURL: String    = s"$companyAuthUrl/gg/sign-in"
    val continueURL: String = appConfig.loginCallback(continueUrl)

    Redirect(loginURL, Map("continue" -> Seq(continueURL), "origin" -> Seq(origin)))

  }

}

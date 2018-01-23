package util

import java.net.URL
import javax.inject.{Inject, Singleton}

import config.AppConfig
import play.api.Logger

import scala.util.Try

trait BackUrlValidator {
  def validate(backUrl : String) : Boolean
}

@Singleton
class ConfigurationBasedBackUrlValidator @Inject()(appConfig: AppConfig) extends BackUrlValidator {

  def validate(backUrl : String) : Boolean = {

    val parsedUrl = Try(new URL(backUrl)).toOption.toRight(left = "Unparseable URL")
    val validationResult = parsedUrl.right.flatMap(checkDomainOnWhitelist)

    validationResult match {
      case Right(_) => true
      case Left(reason) =>
        Logger.error(s"Back URL validation failed. URL: $backUrl, reason: $reason.")
        false
    }

  }

  private def checkDomainOnWhitelist(url : URL) : Either[String, Unit] = {
    val host = url.getHost
    if (appConfig.returnUrlHostnameWhitelist.contains(host)) {
      Right(())
    } else {
      Left("URL contains domain name not from the whitelist")
    }
  }

}

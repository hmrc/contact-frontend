/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import java.net.URL
import javax.inject.{Inject, Singleton}

import config.AppConfig
import play.api.Logging

import scala.util.Try

trait BackUrlValidator {
  def validate(backUrl: String): Boolean
}

@Singleton
class ConfigurationBasedBackUrlValidator @Inject() (appConfig: AppConfig) extends BackUrlValidator with Logging {

  val destinationWhitelist: Set[URL] = appConfig.backUrlDestinationWhitelist.map(new URL(_))

  def validate(backUrl: String): Boolean = {

    val parsedUrl        = Try(new URL(backUrl)).toOption.toRight(left = "Unparseable URL")
    val validationResult = parsedUrl.right.flatMap(checkDomainOnWhitelist)

    validationResult match {
      case Right(_)     => true
      case Left(reason) =>
        logger.error(s"Back URL validation failed. URL: $backUrl, reason: $reason.")
        false
    }

  }

  private def checkDomainOnWhitelist(url: URL): Either[String, Unit] = {
    val host = url.getHost
    if (destinationWhitelist.exists(destinationMatches(_, url))) {
      Right(())
    } else {
      Left(s"URL contains domain name not from the whitelist ($host)")
    }
  }

  private def destinationMatches(url1: URL, url2: URL): Boolean =
    url1.getHost == url2.getHost &&
      url1.getProtocol == url2.getProtocol &&
      url1.getPort == url2.getPort

}

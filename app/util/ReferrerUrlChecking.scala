/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import play.api.Logging
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._
import uk.gov.hmrc.play.bootstrap.binders._

trait ReferrerUrlChecking extends Logging {

  def maybeLogInvalidUrl(service: Option[String], url: RedirectUrl)(implicit appConfig: AppConfig): RedirectUrl =
    url.getEither(appConfig.urlPolicy) match {
      case Right(safeRedirectUrl: SafeRedirectUrl) => RedirectUrl(safeRedirectUrl.url)
      case Left(unsafeRedirectError)               =>
        logger.warn(s"Service [$service] - $unsafeRedirectError")
        RedirectUrl(url.get(UnsafePermitAll).url)
    }

  def maybeSafeRedirectUrl(
    service: Option[String],
    referrerUrl: Option[RedirectUrl],
    headerRetriever: RefererHeaderRetriever
  )(implicit request: Request[AnyContent], appConfig: AppConfig): Option[RedirectUrl] =
    (referrerUrl orElse headerRetriever.refererFromHeaders.map(RedirectUrl(_)))
      .map(maybeLogInvalidUrl(service, _))
}

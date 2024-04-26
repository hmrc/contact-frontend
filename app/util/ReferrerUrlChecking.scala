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

import config.CFConfig
import play.api.Logging
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._
import uk.gov.hmrc.play.bootstrap.binders._

trait ReferrerUrlChecking extends Logging {

  def logInvalidUrl(url: RedirectUrl)(implicit appConfig: CFConfig): SafeRedirectUrl =
    url.getEither(appConfig.urlPolicy) match {
      case Right(safeRedirectUrl: SafeRedirectUrl) => safeRedirectUrl
      case Left(unsafeRedirectError)               =>
        logger.warn(unsafeRedirectError) // probably want to log service name too
        RedirectUrl(url.unsafeValue).get(UnsafePermitAll) // is there a simpler way to do this?
    }

}

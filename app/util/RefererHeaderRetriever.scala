/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.http.HeaderNames
import play.api.mvc.{AnyContent, Request}

import javax.inject.Inject

class RefererHeaderRetriever @Inject() (appConfig: AppConfig) extends HeaderNames {

  // This helper checks config to see if retrieval of referer header is enabled, and if so retrieve from headers.
  // This allows the fallback behaviour of retrieving referer header to be centrally flagged on or off.
  def refererFromHeaders(implicit request: Request[AnyContent]): Option[String] =
    if (appConfig.useRefererFromRequest) request.headers.get(REFERER) else None
}

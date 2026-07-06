/*
 * Copyright 2026 HM Revenue & Customs
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

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.apache.pekko.stream.Materializer
import play.api.mvc.*

class XRobotsTagFilter @Inject() (appConfig: AppConfig)(implicit val mat: Materializer, ec: ExecutionContext)
    extends Filter {

  // This filter adds the ("x-robots-tag" -> "noindex, nofollow") header to all responses. This is to prevent indexing by
  // search engines, in particular when the contact forms are served on domains other than the tax domain via URL masking
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    nextFilter(requestHeader).map { result =>
      if (appConfig.addXRobotsTagHeaderToResponse) result.withHeaders("x-robots-tag" -> "noindex, nofollow") else result
    }
}

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

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.apache.pekko.stream.Materializer
import play.api.mvc.*

class XRobotsTagFilter @Inject() ()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  // This filter adds the ("X-Robots-Tag" -> "noindex, nofollow") header to all responses. This is to prevent indexing
  // by search engines, in particular when the contact forms are served on domains other than the tax domain via URL masking
  // Note that with the current site configuration, if X-Robots-Tag is added via mdtp-frontend-routes, mdtp-frontend-routes
  // will overwrite headers set by the service, i.e. by this filter.
  // This filter can be enabled in config using `play.filters.enabled += util.XRobotsTagFilter`
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    nextFilter(requestHeader).map { result =>
      result.withHeaders("X-Robots-Tag" -> "noindex, nofollow")
    }
}

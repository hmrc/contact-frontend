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

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.inject.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results
import play.api.routing.Router
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}

class XRobotsTagFilterSpec extends AnyWordSpec with Matchers with OptionValues {

  "XRobotsTagFilter" should {

    "not add header to response when no explicit configuration found" in {
      val app    = appWithAdditionalConfiguration()
      val result = route(app, FakeRequest("GET", "some-contact-form")).value

      app.configuration.keys should not(contain("addXRobotsTagHeaderToResponse"))
      headers(result)        should not(contain("X-Robots-Tag" -> "noindex, nofollow"))
    }

    "add header to response when explicitly enabled in configuration" in {
      val app    = appWithAdditionalConfiguration(Map("addXRobotsTagHeaderToResponse" -> "true"))
      val result = route(app, FakeRequest("GET", "some-contact-form")).value
      headers(result) should contain("X-Robots-Tag" -> "noindex, nofollow")
    }

    "not add header to response when explicitly disabled in configuration" in {
      val app    = appWithAdditionalConfiguration(Map("addXRobotsTagHeaderToResponse" -> "false"))
      val result = route(app, FakeRequest("GET", "some-contact-form")).value
      headers(result) should not(contain("X-Robots-Tag" -> "noindex, nofollow"))
    }
  }

  def appWithAdditionalConfiguration(additionalConfiguration: Map[String, String] = Map.empty): Application = {

    import play.api.routing.sird.*

    val Action = stubControllerComponents().actionBuilder

    new GuiceApplicationBuilder()
      .router(Router.from { case GET(p"/some-contact-form") =>
        Action(Results.Ok)
      })
      .configure(additionalConfiguration ++ Map("metrics.jvm" -> false, "metrics.enabled" -> false))
      .build()
  }
}

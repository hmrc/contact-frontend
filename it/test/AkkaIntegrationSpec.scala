/*
 * Copyright 2023 HM Revenue & Customs
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

package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class AkkaIntegrationSpec extends AnyWordSpec with Matchers with GuiceOneServerPerSuite {
  private def baseA11yUrl    = s"http://localhost:$port/contact/accessibility-unauthenticated?service=pay-frontend"
  private def longA11yUrl    = s"$baseA11yUrl&referrerUrl=https%3A%2F%2Fexample.com%2F" + ("x" * 10000)
  private def longestA11yUrl = s"$baseA11yUrl&referrerUrl=https%3A%2F%2Fexample.com%2F" + ("x" * 20000)

  private def baseFeedbackUrl    = s"http://localhost:$port/contact/beta-feedback?service=pay-frontend"
  private def longFeedbackUrl    = s"$baseFeedbackUrl&referrerUrl=https%3A%2F%2Fexample.com%2F" + ("x" * 10000)
  private def longestFeedbackUrl = s"$baseFeedbackUrl&referrerUrl=https%3A%2F%2Fexample.com%2F" + ("x" * 20000)

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  "The Play Framework Akka configuration" should {

    "respond with 200 when given a very long valid URL for accessibility" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response =
        await(wsClient.url(longA11yUrl).get())

      response.status should be(OK)
    }

    "respond with 414 when given an extremely long valid URL for accessibility" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response =
        await(wsClient.url(longestA11yUrl).get())

      response.status should be(REQUEST_URI_TOO_LONG)
    }

    "respond with 200 when given a very long valid URL for beta-feedback" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response =
        await(wsClient.url(longFeedbackUrl).get())

      response.status should be(OK)
    }

    "respond with 414 when given an extremely long valid URL for beta-feedback" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val response =
        await(wsClient.url(longestFeedbackUrl).get())

      response.status should be(REQUEST_URI_TOO_LONG)
    }
  }
}

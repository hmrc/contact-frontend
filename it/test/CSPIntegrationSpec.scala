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

package test

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.FakeRequest

import scala.language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration._

class CSPIntegrationSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  private val fakeRequest = FakeRequest(
    GET,
    "/contact/contact-hmrc-unauthenticated?service=foo"
  )

  "Play Framework CSP configuration" should {
    "respond with the correct CSP header" in {
      val response: Result = Await.result(route(app, fakeRequest).get, 1 seconds)

      response.header.status shouldBe OK

      val headers = response.header.headers

      headers(
        CONTENT_SECURITY_POLICY
      ) should fullyMatch regex "script-src 'nonce-[^']+' 'self' 'unsafe-inline' 'strict-dynamic' ['sha256-[^']+']+ https: http:; object-src 'none'; base-uri 'none'"
    }
  }
}

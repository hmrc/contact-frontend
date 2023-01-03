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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames.{HOST, ORIGIN}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsNull
import play.api.mvc.Result
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._

import scala.language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration._

class CorsIntegrationSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  private val fakeRequest = FakeRequest(
    OPTIONS,
    "/contact/contact-hmrc",
    FakeHeaders(
      Seq(
        ORIGIN                         -> "https://ewf.companieshouse.gov.uk",
        HOST                           -> "localhost",
        ACCESS_CONTROL_REQUEST_HEADERS -> USER_AGENT,
        ACCESS_CONTROL_REQUEST_METHOD  -> POST
      )
    ),
    JsNull
  )

  "Play Framework CORS configuration" should {

    "respond with 200 for allowed origin" in {
      val optionsRequest = fakeRequest

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe OK

      val headers = optionsResponse.header.headers

      headers(ACCESS_CONTROL_ALLOW_HEADERS) shouldBe USER_AGENT.toLowerCase
      headers(ACCESS_CONTROL_ALLOW_METHODS) shouldBe POST
      headers(ACCESS_CONTROL_ALLOW_ORIGIN)  shouldBe "https://ewf.companieshouse.gov.uk"
    }

    "respond with 403 for a disallowed origin" in {
      val optionsRequest = fakeRequest.withHeaders(ORIGIN -> "http://bad.domain.com")

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe FORBIDDEN
    }

    "respond with 403 for a disallowed http method" in {
      val optionsRequest = fakeRequest.withHeaders(ACCESS_CONTROL_REQUEST_METHOD -> DELETE)

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe FORBIDDEN
    }

    "respond with 403 for a disallowed http header" in {
      val optionsRequest = fakeRequest.withHeaders(ACCESS_CONTROL_REQUEST_HEADERS -> "disallowed header")

      val optionsResponse: Result = Await.result(route(app, optionsRequest).get, 1 seconds)

      optionsResponse.header.status shouldBe FORBIDDEN
    }
  }
}

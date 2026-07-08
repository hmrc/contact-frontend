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

import org.apache.pekko.stream.Materializer
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, Results}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.ExecutionContext

class XRobotsTagFilterSpec extends AnyWordSpec with Matchers with OptionValues with GuiceOneAppPerSuite {

  implicit val materializer: Materializer = app.materializer
  implicit val ec: ExecutionContext       = ExecutionContext.global

  val filter = new XRobotsTagFilter()

  "XRobotsTagFilter" should {

    "add the expected header to the response when there are no other headers" in {
      val okAction: EssentialAction = _ => Accumulator.done(Results.Ok)
      val request                   = FakeRequest("GET", "/some-contact-frontend")
      val result                    = filter.apply(okAction)(request)

      status(result)     shouldBe OK
      headers(result)      should contain("X-Robots-Tag" -> "noindex, nofollow")
      headers(result).size should be(1)
    }

    "add the expected header to the response when there are already headers set" in {
      val originalHeader            = ("service-name", "some-service")
      val okAction: EssentialAction = _ => Accumulator.done(Results.Ok.withHeaders(originalHeader))

      val request = FakeRequest("GET", "/some-contact-frontend")
      val result  = filter.apply(okAction)(request)

      status(result)     shouldBe OK
      headers(result)      should contain("X-Robots-Tag" -> "noindex, nofollow")
      headers(result)      should contain(originalHeader)
      headers(result).size should be(2)
    }

    "overwrite when there is already X-Robots-Tag header set" in {
      // This test to document what happens when we set a header with the same name as an existing header
      val originalHeader            = ("X-Robots-Tag", "some-other-value")
      val okAction: EssentialAction = _ => Accumulator.done(Results.Ok.withHeaders(originalHeader))

      val request = FakeRequest("GET", "/some-contact-frontend")
      val result  = filter.apply(okAction)(request)

      status(result)     shouldBe OK
      headers(result)      should contain("X-Robots-Tag" -> "noindex, nofollow")
      headers(result)      should not(contain(originalHeader))
      headers(result).size should be(1)
    }
  }

}

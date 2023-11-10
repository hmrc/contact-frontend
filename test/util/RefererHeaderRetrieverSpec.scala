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

package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import play.api.http.HeaderNames._

class RefererHeaderRetrieverSpec extends AnyWordSpec with Matchers {

  "Given a referer header retrieve, calling retrieve" should {

    "return some referer header if header in request" in {
      val retriever = new RefererHeaderRetriever
      val request   = FakeRequest().withHeaders((REFERER, "some-service-url"))

      val refererHeader = retriever.refererFromHeaders(request)
      refererHeader shouldBe Some("some-service-url")
    }

    "return none if header not present" in {
      val retriever = new RefererHeaderRetriever
      val request   = FakeRequest()

      val refererHeader = retriever.refererFromHeaders(request)
      refererHeader shouldBe None
    }
  }
}

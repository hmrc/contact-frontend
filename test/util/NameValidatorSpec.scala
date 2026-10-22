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

class NameValidatorSpec extends AnyWordSpec with Matchers {
  import NameValidator._

  "Given a name validator, calling validate" should {

    "return true if a name contains only letters" in {
      val validName = "Firstname Lastname"
      validate(validName) shouldBe true
    }

    "return true if a name contains allowed punctuation" in {
      val validName            = "Firstname Some-Lastname"
      val validNameWithAddress = "BetterThanTinder www.example.com"

      validate(validName)            shouldBe true
      validate(validNameWithAddress) shouldBe true
    }

    "return false if a name contains punctuation other than allowed" in {
      val invalidName = "BetterThanTinder some:example:here"
      validate(invalidName) shouldBe false
    }

    "return false if name contains http:// or https://" in {
      val validCustomName  = "BetterThanTinder some example here"
      val invalidNameHttp  = "BetterThanTinder http://www.example.com"
      val invalidNameHttps = "BetterThanTinder https://www.example.com"

      validate(validCustomName)  shouldBe true
      validate(invalidNameHttp)  shouldBe false
      validate(invalidNameHttps) shouldBe false
    }
  }
}

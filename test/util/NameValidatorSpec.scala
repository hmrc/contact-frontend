/*
 * Copyright 2021 HM Revenue & Customs
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

  "Given a name validator, calling validate" should {

    val validator = NameValidator()

    "return true if a name contains only letters" in {
      val validName = "Firstname Lastname"
      validator.validate(validName) shouldBe true
    }

    "return true if a name contains allowed punctuation" in {
      val validName            = "Firstname Some-Lastname"
      val validNameWithAddress = "BetterThanTinder www.example.com"

      validator.validate(validName)            shouldBe true
      validator.validate(validNameWithAddress) shouldBe true
    }

    "return false if a name contains punctuation other than allowed" in {
      val invalidName = "BetterThanTinder some:example:here"
      validator.validate(invalidName) shouldBe false
    }

    "return false if name contains http:// or https://" in {
      val customValidator = new NameValidator {
        override val nameRegex = """.*"""
      }

      val validCustomName  = "BetterThanTinder some:example:here"
      val invalidNameHttp  = "BetterThanTinder http://www.example.com"
      val invalidNameHttps = "BetterThanTinder https://www.example.com"

      customValidator.validate(validCustomName)  shouldBe true
      customValidator.validate(invalidNameHttp)  shouldBe false
      customValidator.validate(invalidNameHttps) shouldBe false
    }
  }
}

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

class DeskproEmailValidatorSpec extends AnyWordSpec with Matchers {

  private val emailValidator = new DeskproEmailValidator()

  "Given a valid email address, an email validator" should {
    "return true" in {
      emailValidator.validate("some.name@foo.com")   shouldBe true
      emailValidator.validate("some.name@foo.co.uk") shouldBe true
      emailValidator.validate("some_name@foo.biz")   shouldBe true
    }
  }

  "Given a string that is not an email address, an email validator" should {
    "return false" in {
      emailValidator.validate("n/a")            shouldBe false
      emailValidator.validate("hello.world")    shouldBe false
      emailValidator.validate("Not applicable") shouldBe false
    }
  }

  "Given an email address with an invalid domain, an email validator" should {
    "return false" in {
      emailValidator.validate("some.name@o.2")     shouldBe false
      emailValidator.validate("some.name@moo.min") shouldBe false
      emailValidator.validate("some.name@foo")     shouldBe false
    }
  }
}

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

import model.DateOfBirth
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DateOfBirthValidatorSpec extends AnyWordSpec with Matchers {

  "Given a date of birth validator, calling isValidDate" should {
    val validator = DateOfBirthValidator()

    "return true if the date of birth is a valid date" in {
      val validDate = DateOfBirth("01", "01", "1990")
      validator.isValidDate(validDate) shouldBe true
    }

    "return false if the date of birth is not a real date" in {
      val invalidDay   = DateOfBirth("31", "02", "1990")
      val invalidMonth = DateOfBirth("01", "13", "1990")
      val invalidYear  = DateOfBirth("01", "01", "sausage")

      validator.isValidDate(invalidDay)   shouldBe false
      validator.isValidDate(invalidMonth) shouldBe false
      validator.isValidDate(invalidYear)  shouldBe false
    }
  }

  "Given a date of birth validator, calling isNotFutureDate" should {
    val validator = DateOfBirthValidator()

    "return true if the date of birth is a in the past" in {
      val pastDate = DateOfBirth("01", "01", "1990")
      validator.isNotFutureDate(pastDate) shouldBe true
    }

    "return false if the date of birth is in the future" in {
      val futureDate = DateOfBirth("01", "01", "2035")
      validator.isNotFutureDate(futureDate) shouldBe false
    }
  }
}

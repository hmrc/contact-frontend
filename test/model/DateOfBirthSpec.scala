/*
 * Copyright 2025 HM Revenue & Customs
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

package model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DateOfBirthSpec extends AnyWordSpec with Matchers {

  "Given a date of birth validator, calling isValidDate" should {

    "return true if the date of birth is a valid date" in {
      val validDate = DateOfBirth("01", "01", "1990")
      validDate.isValidDate() shouldBe true
    }

    "return false if the date of birth is not a real date" in {
      val invalidDay   = DateOfBirth("31", "02", "1990")
      val invalidMonth = DateOfBirth("01", "13", "1990")
      val invalidYear  = DateOfBirth("01", "01", "sausage")

      invalidDay.isValidDate()   shouldBe false
      invalidMonth.isValidDate() shouldBe false
      invalidYear.isValidDate()  shouldBe false
    }
  }

  "Given a date of birth validator, calling isNotFutureDate" should {

    "return true if the date of birth is a in the past" in {
      val pastDate = DateOfBirth("01", "01", "1990")
      pastDate.isNotFutureDate() shouldBe true
    }

    "return false if the date of birth is in the future" in {
      val futureDate = DateOfBirth("01", "01", "2035")
      futureDate.isNotFutureDate() shouldBe false
    }
  }
}

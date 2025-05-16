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

package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TaxIdentifierValidatorSpec extends AnyWordSpec with Matchers {

  "Given a tax identifier validator, validating a NINO" should {
    val validator = TaxIdentifierValidator()

    "return true if passed a valid NINO" in {
      val validNino = "AA 11 22 33 B"
      validator.validateNino(validNino) shouldBe true
    }

    "return false if passed an invalid NINO" in {
      val invalidNino = "AA 11 FALSE"
      validator.validateNino(invalidNino) shouldBe false
    }
  }

  "Given a tax identifier validator, validating a SA UTR" should {
    val validator = TaxIdentifierValidator()

    "return true if no SA UTR passed in" in {
      validator.validateSaUtr("") shouldBe true
    }

    "return true if passed a valid SA UTR" in {
      val validSaUtr = "1234567890"
      validator.validateSaUtr(validSaUtr) shouldBe true
    }

    "return true if passed a valid SA UTR with spaces" in {
      val validSaUtr = " 12 34 56 78 90 "
      validator.validateSaUtr(validSaUtr) shouldBe true
    }

    "return false if passed an invalid SA UTR" in {
      val tooShortSaUtr = "12345678"
      val tooLongSaUtr  = "1234567890123"

      validator.validateSaUtr(tooShortSaUtr) shouldBe false
      validator.validateSaUtr(tooLongSaUtr)  shouldBe false
    }
  }
}

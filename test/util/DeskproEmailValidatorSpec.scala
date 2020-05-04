/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DeskproEmailValidatorSpec extends AnyWordSpec with Matchers {

  private val emailValidator = new DeskproEmailValidator()

  "Given a valid email address, an email validator" should {
    "return true" in {
      emailValidator.validate("some.name@foo.com") shouldBe true
      emailValidator.validate("some.name@foo.co.uk") shouldBe true
      emailValidator.validate("some_name@foo.biz") shouldBe true
    }
  }

  "Given a string that is not an email address, an email validator" should {
    "return false" in {
      emailValidator.validate("n/a") shouldBe false
      emailValidator.validate("hello.world") shouldBe false
      emailValidator.validate("Not applicable") shouldBe false
    }
  }

  "Given an email address with an invalid domain, an email validator" should {
    "return false" in {
      emailValidator.validate("some.name@o.2") shouldBe false
      emailValidator.validate("some.name@moo.min") shouldBe false
      emailValidator.validate("some.name@foo") shouldBe false
    }
  }
}

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

class DeskproEmailValidatorSpec extends AnyWordSpec with Matchers {

  private val emailValidator = DeskproEmailValidator()

  "Given a valid email address, an email validator" should {
    "return true" in {
      emailValidator.validate("some.name@foo.com")     shouldBe true
      emailValidator.validate("some.name@foo.co.uk")   shouldBe true
      emailValidator.validate("some_name@foo.biz")     shouldBe true
      emailValidator.validate("somename@foo.com")      shouldBe true
      emailValidator.validate("s@f.c")                 shouldBe true
      emailValidator.validate("SOMENAME@foo.com")      shouldBe true
      emailValidator.validate("SOMENAME@FOO.COM")      shouldBe true
      emailValidator.validate("some.name@mailserver1") shouldBe true
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
      emailValidator.validate("some.name@foo.")        shouldBe false
      emailValidator.validate("some.name@.com")        shouldBe false
      emailValidator.validate("some_name@foo_bar.com") shouldBe false
      emailValidator.validate("some.name@foo.com@")    shouldBe false
    }
  }

  "Given an email address with a valid IP address, an email validator" should {
    "return true" in {
      emailValidator.validate("some.name@01.01.01.01")   shouldBe true
      emailValidator.validate("some.name@251.12.19.223") shouldBe true
    }
  }

  "Given an email address with an invalid IP address, an email validator" should {
    "return true" in {
      emailValidator.validate("some.name@111.222.333.444") shouldBe false
      emailValidator.validate("some.name@251.12.19")       shouldBe false
    }
  }

  object AgreeingWithDeskproValidation {
    val validEmails = List(
      "fabien@symfony.com",
      "example@example.co.uk",
      "fabien_potencier@example.fr",
      "example@localhost",
      "fab\'ien@symfony.com",
      "fabien+@symfony.com",
      "test@email.com.au"
    )

    val invalidEmails = List(
      "test@example.com test",
      "user name@example.com",
      "example.@example.co.uk",
      "example@example@example.co.uk",
      "(test_exampel@example.fr)",
      "example(example)example@example.co.uk",
      ".example@localhost",
      "example@localhost\\",
      "example@localhost.",
      "username@ example.com",
      "example@(fake).com",
      "example@(fake.com",
      "username@example,com",
      "usern,ame@example.com",
      "user[na]me@example.com",
      """"@iana.org"",
      ""\"@iana.org",
      ""test"test@iana.org",
      ""test""test"@iana.org",
      ""test"."test"@iana.org",
      ""test".test@iana.org",
      ""test"'.chr(0).'@iana.org",
      ""test\"@iana.org",
      "hr(226).'@iana.org",
      "test@'.chr(226).'.org",
      ""\r\ntest@iana.org"",
      ""\r\n test@iana.org"",
      ""\r\n \r\ntest@iana.org"",
      ""\r\n \r\ntest@iana.org"",
      ""\r\n \r\n test@iana.org"",
      "test@iana.org \r\n",
      "test@iana.org \r\n ",
      "test@iana.org \r\n \r\n",
      "test@iana.org \r\n\r\n",
      "test@iana.org \r\n\r\n ",
      "test@iana/icann.org",
      "test@foo;bar.com",
      "test;123@foobar.com",
      "test@example..com",
      "email.email@email.""",
      "test@email>",
      "test@email<",
      "test@email{",
      "test@email.com]",
      "test@ema[il.com"
    ) ++ List(
      "@example.co.uk",
      "example@",
      "example@example-.co.uk",
      "example@example-",
      "example@@example.co.uk",
      "example..example@example.co.uk",
      "example@example..co.uk",
      "<fabien_potencier>@example.fr",
      "example@.localhost",
      "(example@localhost",
      "comment)example@localhost",
      "example(comment))@localhost",
      "example@comment)localhost",
      "example@localhost(comment))",
      "example@(comment))example.com",
      "\"example@localhost",
      "exa\"mple@localhost",
      "exampl\ne@example.co.uk",
      "example@[[]",
      "exampl\te@example.co.uk",
      "example@exa\rmple.co.uk",
      "example@[\r]",
      "exam\rple@example.co.uk"
    )

    val invalidWithWarningsEmails = List(
      "example @invalid.example.com",
      "example@ invalid.example.com",
      "example@invalid.example(examplecomment).com",
      "example(examplecomment)@invalid.example.com",
      "\"\t\"@invalid.example.com",
      "\"\r\"@invalid.example.com",
      "example@[127.0.0.1]",
      "example@[IPv6:2001:0db8:85a3:0000:0000:8a2e:0370:7334]",
      "example@[IPv6:2001:0db8:85a3:0000:0000:8a2e:0370::]",
      "example@[IPv6:2001:0db8:85a3:0000:0000:8a2e:0370:7334::]",
      "example@[IPv6:1::1::1]",
      "example@[\n]",
      "example@[::1]",
      "example@[::123.45.67.178]",
      "example@[IPv6::2001:0db8:85a3:0000:0000:8a2e:0370:7334]",
      "example@[IPv6:z001:0db8:85a3:0000:0000:8a2e:0370:7334]",
      "example@[IPv6:2001:0db8:85a3:0000:0000:8a2e:0370:]",
      "\"example\"@invalid.example.com"
    )
  }

  object DisagreeingWithDeskproValidation {
    // feels fairly trivial to change regex to accept unicode characters
    // examples containing spaces, quotes and brackets are also fixable
    // examples with special characters *!&^%$ at the end seem unlikely, so not worth fixing
    val validButRejectedEmails = List(
      "â@iana.org",
      "fab\\ ien@symfony.com",
      "example((example))@fakedfake.co.uk",
      "example@faked(fake).co.uk",
      "инфо@письмо.рф",
      "username\"@example.com",
      "\"user,name\"@example.com",
      "\"user name\"@example.com",
      "\"user@name\"@example.com",
      "\"a\"@iana.org",
      "\"test test\"@iana.org",
      "\"\"@iana.org",
      "\"\\\"\"@iana.org",
      "müller@möller.de",
      "test@email*",
      "test@email!",
      "test@email&",
      "test@email^",
      "test@email%",
      "test@email$",
      "1500111@профи-инвест.рф"
    )

    // these feel contrived and unlikely to occur in the wild
    val invalidButAcceptedEmails = List(
      "ex\\ample@localhost",
      "example@local\\host"
    )

    // these feel contrived and unlikely to occur in the wild
    val invalidWithWarningsButAcceptedEmails = List(
      "too_long_localpart_too_long_localpart_too_long_localpart_too_long_localpart@invalid.example.com",
      "example@toolonglocalparttoolonglocalparttoolonglocalparttoolonglocalpart.co.uk",
      "example@toolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalpart",
      "example@toolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalparttoolonglocalpar",
      "test@test"
    )
  }

  "DeskproEmailValidator" should {
    AgreeingWithDeskproValidation.validEmails.foreach { email =>
      s"accept valid email address: $email" in {
        emailValidator.validate(email) shouldBe true
      }
    }

    DisagreeingWithDeskproValidation.validButRejectedEmails.foreach { email =>
      s"accept valid email address: $email" in pendingUntilFixed {
        emailValidator.validate(email) shouldBe true
      }
    }

    AgreeingWithDeskproValidation.invalidEmails.foreach { email =>
      s"reject invalid email address: $email" in {
        emailValidator.validate(email) shouldBe false
      }
    }

    DisagreeingWithDeskproValidation.invalidButAcceptedEmails.foreach { email =>
      s"reject invalid email address: $email" in pendingUntilFixed {
        emailValidator.validate(email) shouldBe false
      }
    }

    AgreeingWithDeskproValidation.invalidWithWarningsEmails.foreach { email =>
      s"reject invalid email address: $email" in {
        emailValidator.validate(email) shouldBe false
      }
    }

    DisagreeingWithDeskproValidation.invalidWithWarningsButAcceptedEmails.foreach { email =>
      s"reject invalid email address: $email" in pendingUntilFixed {
        emailValidator.validate(email) shouldBe false
      }
    }
  }

}

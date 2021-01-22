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

import config.AppConfig
import org.mockito.Mockito
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec

class ConfigurationBasedBackUrlValidatorSpec extends AnyWordSpec with Matchers with MockitoSugar {

  val appConfig = mock[AppConfig]

  Mockito
    .when(appConfig.backUrlDestinationWhitelist)
    .thenReturn(Set("http://tax.service.gov.uk", "https://tax2.service.gov.uk", "http://localhost:8080"))

  val validator = new ConfigurationBasedBackUrlValidator(appConfig)

  "Back URL validator" should {

    "properly filter out back button URL's" in {

      val urls = Table(
        ("URL", "Expected validation result"),
        ("http://tax.service.gov.uk/1234", true),
        ("http://tax2.service.gov.uk", false),
        ("https://tax2.service.gov.uk", true),
        ("http://localhost:8080/1234", true),
        ("http://localhost/1234", false),
        ("//$1234", false),
        ("http://tax.service.gov.uk.invalid/1234", false),
        ("http://www.tax.service.gov.uk@self-assessment:evil.com/pay-online", false),
        ("http://www.tax.service.gov.uk@self-assessment.evil.com/pay-online", false),
        ("http://www.tax.service.gov.uk:self-assessment@evil.com/pay-online", false),
        ("/test", false),
        ("/", false),
        ("../", false),
        ("test", false),
        ("", false),
        ("mailto:evil@evil.com", false)
      )

      forAll(urls) { (url, expectedResult) =>
        validator.validate(url) shouldBe expectedResult
      }

    }
  }

}

/*
 * Copyright 2020 HM Revenue & Customs
 *
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

  Mockito.when(appConfig.backUrlDestinationWhitelist).thenReturn(Set(
    "http://tax.service.gov.uk",
    "https://tax2.service.gov.uk",
    "http://localhost:8080"))

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

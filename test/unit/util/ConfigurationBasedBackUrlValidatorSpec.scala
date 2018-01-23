package unit.util

import config.AppConfig
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import util.ConfigurationBasedBackUrlValidator

class ConfigurationBasedBackUrlValidatorSpec extends UnitSpec with MockitoSugar {

  val appConfig = mock[AppConfig]
  val validator = new ConfigurationBasedBackUrlValidator(appConfig)

  "Back URL validator" should {
    "reject invalid URL" in {
      Mockito.when(appConfig.returnUrlHostnameWhitelist).thenReturn(Set("tax.service.gov.uk"))
      validator.validate("//$1234") shouldBe false
    }

    "reject URL with non whitelisted domain name" in {
      Mockito.when(appConfig.returnUrlHostnameWhitelist).thenReturn(Set("tax.service.gov.uk", "tax2.service.gov.uk"))
      validator.validate("http://tax.service.gov.uk.invalid/1234") shouldBe false
    }

    "accept URLs with whitelisted domain name" in {
      Mockito.when(appConfig.returnUrlHostnameWhitelist).thenReturn(Set("tax.service.gov.uk", "tax2.service.gov.uk"))
      validator.validate("http://tax.service.gov.uk/1234") shouldBe true
      validator.validate("https://tax2.service.gov.uk") shouldBe true
    }
  }

}

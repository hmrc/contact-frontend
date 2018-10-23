package util

import org.scalacheck.{Gen, Shrink}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import uk.gov.hmrc.http.CookieNames

class DeviceIdBucketCalculatorSpec extends WordSpec with Matchers with PropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfig(minSize=100000, maxSize = 200000)
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  "Bucket id should be generated consistently nad within range of 0 to 99" in {

    forAll(Gen.alphaNumStr) { deviceId =>

        val requestWithDeviceId = FakeRequest.apply().withCookies(Cookie(CookieNames.deviceID, deviceId))

        val bucket1 = BucketCalculator.deviceIdBucketCalculator(requestWithDeviceId)
        val bucket2 = BucketCalculator.deviceIdBucketCalculator(requestWithDeviceId)

        bucket1 shouldBe bucket2
        bucket1 should be >= 0
        bucket2 should be < 100

      }
  }

  "Bucket id for empty deviceID should be 0 " in {
    val requestWithoutDeviceId = FakeRequest.apply()

    BucketCalculator.deviceIdBucketCalculator(requestWithoutDeviceId) shouldBe 0
  }

}

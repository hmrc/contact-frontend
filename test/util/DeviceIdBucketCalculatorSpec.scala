package util

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.CookieNames

class DeviceIdBucketCalculatorSpec extends WordSpec with Matchers with PropertyChecks {

  "Bucket id should be generated consistently nad within range of 0 to 99" in {

    forAll(Gen.alphaNumStr) { deviceId =>

        val requestWithDeviceId = FakeRequest.apply().withCookies(Cookie(CookieNames.deviceID, deviceId))

        val bucket1 = BucketCalculator.deviceIdBucketCalculator(requestWithDeviceId)
        val bucket2 = BucketCalculator.deviceIdBucketCalculator(requestWithDeviceId)

        bucket1 shouldBe bucket2
        bucket1 should be >= 0
        bucket2 should be < 99

      }
  }

  "Bucket id for empty deviceID should be 0 " in {
    val requestWithoutDeviceId = FakeRequest.apply()

    BucketCalculator.deviceIdBucketCalculator(requestWithoutDeviceId) shouldBe 0
  }


  "Device ID should be assigned from 1 to 99 with roughly equal probability distribution (chi-squared 99% certainty for 1000 degrees of freedom)" in {
    def generateTestRequests(deviceIdCount: Int,
                             minDeviceIdPadLength: Int,
                             maxDeviceIdPadLength: Int,
                             validChars: String,
                             deviceIdPrefix: String = "",
                             deviceIdSuffix: String = ""): List[FakeRequest[AnyContentAsEmpty.type]] = {

      def randomString(minLength: Int, maxLength: Int): String = {
        def randomNumberBetween(min: Int, max: Int): Int = min + scala.util.Random.nextInt(max + 1 - min)
        def randomChar: Char = validChars(randomNumberBetween(0, validChars.length - 1))
        val stringBuilder = new StringBuilder
        if (maxLength > 0) for (i <- 1 to randomNumberBetween(1, randomNumberBetween(minLength, maxLength))) stringBuilder.append(randomChar)
        stringBuilder.toString()
      }

      val testDeviceIds: List[String] = List.fill(deviceIdCount)(s"$deviceIdPrefix${randomString(minDeviceIdPadLength, maxDeviceIdPadLength)}$deviceIdSuffix")
      testDeviceIds.map(deviceId => FakeRequest.apply().withCookies(Cookie(CookieNames.deviceID, deviceId)))
    }

    val testDeviceIdCount: Int = 1001
    val testRequests: List[FakeRequest[AnyContentAsEmpty.type]] =
      generateTestRequests(
        testDeviceIdCount,
        minDeviceIdPadLength = 1,
        maxDeviceIdPadLength = 100,
        validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!$%&'()*+-./:<=>?@[]^_`{|}~",
        deviceIdPrefix = "mdtpi#",
        deviceIdSuffix = "#blah"
      )

    val expectedBucketCount: Double = testDeviceIdCount/100
    val intDistribution: List[(Int, Int)] = testRequests.map(BucketCalculator.deviceIdBucketCalculator(_)).groupBy(x => x).mapValues(_.size).toList
    val squares: List[Double] = for {
      (_, count) <- intDistribution
      deviation = expectedBucketCount - count
      square = deviation * deviation
    } yield square

    val chiSquaredNinetyNinePercentCertaintyBoundary: Double = 898.912
    val pearsonStatistic: Double = squares.sum / expectedBucketCount

    pearsonStatistic should be <= chiSquaredNinetyNinePercentCertaintyBoundary
  }

}

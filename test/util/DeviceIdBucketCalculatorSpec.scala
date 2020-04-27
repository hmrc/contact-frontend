/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package util

import org.scalacheck.{Gen, Shrink}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.CookieNames

class DeviceIdBucketCalculatorSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(minSize=100000, sizeRange = 200000)
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  "Bucket id should be generated consistently and within range of 0 to 99" in {

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

  "Device ID should be assigned from 1 to 99 with roughly equal probability distribution (chi-squared 99.5% certainty for 1000 degrees of freedom)" in {
    def randomString: String = {
      def randomNumberBetween(min: Int, max: Int): Int = min + scala.util.Random.nextInt(max + 1 - min)
      val validChars: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!$%&'()*+-./:<=>?@[]^_`{|}~"
      def randomChar: Char = validChars(randomNumberBetween(0, validChars.length - 1))
      val stringBuilder = new StringBuilder
      for (i <- 1 to randomNumberBetween(1, 100)) stringBuilder.append(randomChar)
      stringBuilder.toString()
    }

    def generateTestDeviceId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest.apply().withCookies(Cookie(CookieNames.deviceID, s"mdtpi#$randomString#blah"))

    val testDeviceIdCount: Int = 1001
    val testBuckets: List[Int] = for (i <- (1 to testDeviceIdCount).toList; buckets = BucketCalculator.deviceIdBucketCalculator(generateTestDeviceId)) yield buckets

    val intDistribution: List[(Int, Int)] = testBuckets.groupBy(x => x).mapValues(_.size).toList
    val expectedBucketCount: Double = testDeviceIdCount/100

    def calculatePearsonsStatistic(distribution: List[(_, Int)], expectation: Double): Double = {
      val squares: List[Double] = for {
        (_, count) <- distribution
        deviation = expectation - count
        square = deviation * deviation
      } yield square

      squares.sum / expectation
    }

    val pearsonStatistic: Double = calculatePearsonsStatistic(intDistribution, expectedBucketCount)
    val ChiSquared1000DegreesFreedom99PercentCertaintyBoundary: Double = 888.564 // https://en.wikibooks.org/wiki/Engineering_Tables/Chi-Squared_Distibution
    pearsonStatistic should be <= ChiSquared1000DegreesFreedom99PercentCertaintyBoundary
  }

}

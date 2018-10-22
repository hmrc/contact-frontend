package util

import org.scalatest.{Matchers, WordSpec}

class ABTestingFeaturePartitionerSpec extends WordSpec with Matchers{
  "GetHelpWithThisPageFeaturePartitioner" when {
    "partitioning" should {
      val testInstance =
        new ABTestingFeaturePartitioner[Any, GetHelpWithThisPageFeature, GetHelpWithThisPageFeature_A, GetHelpWithThisPageFeature_B](50,
          GetHelpWithThisPageFeature_A,
          GetHelpWithThisPageFeature_B)

      "return feature A" in {
        testInstance.partition(49) shouldBe GetHelpWithThisPageFeature_A
      }

      "return feature A, for hashable greater than 100" in {
        testInstance.partition(149) shouldBe GetHelpWithThisPageFeature_A
      }

      "return feature B, lower bound" in {
        testInstance.partition(50) shouldBe GetHelpWithThisPageFeature_B
      }

      "return feature B" in {
        testInstance.partition(51) shouldBe GetHelpWithThisPageFeature_B
      }

      "return feature B, for hashable greater than 100" in {
        testInstance.partition(151) shouldBe GetHelpWithThisPageFeature_B
      }

      "return feature B, for negative hashable" in {
        testInstance.partition(-51) shouldBe GetHelpWithThisPageFeature_B
      }

      "return feature A and B in proportions within 2 standard deviations from expected probability (50%)" in {
        def randomString: String = scala.util.Random.nextString(randomLengthBetween(1, 100))
        def randomLengthBetween(minStringLength: Int, maxStringLength: Int): Int = {
          minStringLength + scala.util.Random.nextInt(maxStringLength + 1 - minStringLength)
        }

        val testStringCount: Int = 1000000
        val testStrings: Seq[String] = Seq.fill(testStringCount)(randomString)
        val featureACount: Int = testStrings.count(testInstance.partition(_) == GetHelpWithThisPageFeature_A)

        val difference: Double = Math.abs(0.5 * testStringCount - featureACount)
        val twoStandardDeviations: Double = Math.sqrt(testStringCount)

        difference should be <= twoStandardDeviations
      }
    }
  }
}

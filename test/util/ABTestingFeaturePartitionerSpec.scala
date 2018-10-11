package util

import org.scalatest.{Matchers, WordSpec}

class ABTestingFeaturePartitionerSpec extends WordSpec with Matchers{
  "GetHelpWithThisPageFeaturePartitioner" when {
    "partitioning" should {
      val testInstance =
        new ABTestingFeaturePartitioner[Int, GetHelpWithThisPageFeature, GetHelpWithThisPageFeature_A, GetHelpWithThisPageFeature_B](50,
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
    }
  }
}

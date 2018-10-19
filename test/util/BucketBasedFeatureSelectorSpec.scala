package util

import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Request
import play.api.test.FakeRequest
import util.BucketCalculator.BucketCalculator


class BucketBasedFeatureSelectorSpec extends WordSpec with Matchers {

  val testFeature1 = GetHelpWithThisPageFeatureFieldHints
  val testFeature2 = GetHelpWithThisPageImprovedFieldValidation

  val mockBucketCalculator: BucketCalculator = request =>
    request.headers.get("bucket").map(_.toInt).getOrElse(0)

  val testInstance = new BucketBasedFeatureSelector(
    mockBucketCalculator,
    Set(FeatureEnablingRule(0, 20, testFeature1),
        FeatureEnablingRule(10, 30, testFeature2)))

  "RequestBasedFeatureSelector" should {

    "return features Test1 and Test2 for user in bucket 10" in {
      val request: Request[AnyRef] = FakeRequest().withHeaders(("bucket", "10"))
      testInstance.computeFeatures(request) shouldBe Set(testFeature1,
                                                         testFeature2)
    }

    "return feature Test1 for user in bucket 9" in {
      val request: Request[AnyRef] = FakeRequest().withHeaders(("bucket", "9"))
      testInstance.computeFeatures(request) shouldBe Set(testFeature1)
    }

    "return feature Test2 for user in bucket 20" in {
      val request: Request[AnyRef] = FakeRequest().withHeaders(("bucket", "20"))
      testInstance.computeFeatures(request) shouldBe Set(testFeature2)
    }

    "return no features for user in bucket 30" in {
      val request: Request[AnyRef] = FakeRequest().withHeaders(("bucket", "30"))
      testInstance.computeFeatures(request) shouldBe Set.empty[Feature]
    }

  }

}

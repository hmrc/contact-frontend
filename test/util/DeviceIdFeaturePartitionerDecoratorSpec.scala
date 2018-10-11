package util

import org.mockito.Mockito
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar.mock
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderNames

class DeviceIdFeaturePartitionerDecoratorSpec extends WordSpec with Matchers {
  val mockFeaturePartitioner: FeaturePartitioner[String, GetHelpWithThisPageFeature] = mock[FeaturePartitioner[String, GetHelpWithThisPageFeature]]

  val testInstance = new DeviceIdFeaturePartitionerDecorator[GetHelpWithThisPageFeature](DeviceIdProvider.deviceIdProvider, mockFeaturePartitioner)
  val deviceId = "Hello, World!"

  val request: Request[AnyRef] = FakeRequest().withHeaders((HeaderNames.deviceID, deviceId))

  "DeviceIdPartitioner" when {

    "partitioning based on DeviceId header" should {
      "return feature A" in {
        expectFeature(GetHelpWithThisPageFeature_A)
      }

      "return feature B" in {
        expectFeature(GetHelpWithThisPageFeature_B)
      }
    }
  }


  private def expectFeature(feature: GetHelpWithThisPageFeature) = {
    Mockito.when(mockFeaturePartitioner.partition(deviceId)).thenReturn(feature)

    testInstance.partition(request) shouldBe feature
  }
}

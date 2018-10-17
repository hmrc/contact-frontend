package util

import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import util.DeviceIdProvider.DeviceIdProvider

trait FeaturePartitioner[H, F <: Feature] {
  def partition(hashable: H): F
}

class ABTestingFeaturePartitioner[H, F <: Feature, F1 <: F, F2 <: F](
    threshold: Int,
    featureA: F1,
    featureB: F2)
    extends FeaturePartitioner[H, F] {
  override def partition(hashable: H): F = {

    val bucket = hashable.hashCode % 100

    val chosenFeature: F =
      if (math.abs(bucket) < threshold) featureA else featureB

    Logger.info(s"Request landed in bucket [$bucket], chosen feature: [$chosenFeature].")

    chosenFeature
  }
}

class DeviceIdFeaturePartitionerDecorator[F <: Feature](
    getDeviceId: DeviceIdProvider,
    decoratedPartitioner: FeaturePartitioner[String, F])
    extends FeaturePartitioner[Request[_], F] {
  override def partition(request: Request[_]): F = {

    val deviceIdMaybe: Option[String] = getDeviceId(
      HeaderCarrierConverter.fromHeadersAndSession(request.headers,
                                                   Some(request.session)))

    decoratedPartitioner.partition(deviceIdMaybe.getOrElse(""))
  }
}

object DeviceIdProvider {
  type DeviceIdProvider = HeaderCarrier => Option[String]

  val deviceIdProvider: DeviceIdProvider = _.deviceID
}
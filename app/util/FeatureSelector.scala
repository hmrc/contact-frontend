package util

import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.play.HeaderCarrierConverter
import util.BucketCalculator.BucketCalculator

trait FeatureSelector {
  def computeFeatures(request: Request[_]): Set[Feature]
}

case class FeatureEnablingRule(bucketFrom: Int,
                               bucketTo: Int,
                               feature: Feature) {
  def isApplicable(bucket: Int): Boolean =
    bucket >= bucketFrom && bucket < bucketTo
}

class BucketBasedFeatureSelector(computeBucket: BucketCalculator,
                                 rules: Iterable[FeatureEnablingRule])
    extends FeatureSelector {

  override def computeFeatures(request: Request[_]): Set[Feature] = {
    val bucket = computeBucket(request)
    val features = computeFeatures(bucket)
    Logger.info(s"Request assigned to the bucket $bucket, features enabled: ${features.map(_.name).mkString(";")}")
    features
  }

  private def computeFeatures(bucket: Int): Set[Feature] =
    (for (rule <- rules if rule.isApplicable(bucket)) yield rule.feature).toSet
}

object BucketCalculator {
  type BucketCalculator = Request[_] => Int

  val deviceIdBucketCalculator: BucketCalculator = { request =>
    val deviceIdMaybe: Option[String] = HeaderCarrierConverter
      .fromHeadersAndSession(request.headers, Some(request.session))
      .deviceID
    val deviceId = deviceIdMaybe.getOrElse("")
    val bucket = math.abs(deviceId.hashCode % 100)
    bucket
  }
}

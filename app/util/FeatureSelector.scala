/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.play.HeaderCarrierConverter
import util.BucketCalculator.BucketCalculator

import scala.util.Try

trait FeatureSelector {
  def computeFeatures(request: Request[_], service: Option[String]): Set[Feature]
}

case class FeatureEnablingRule(bucketFrom: Int, bucketTo: Int, servicesLimit: Option[Set[String]], feature: Feature) {
  def isApplicable(bucket: Int, maybeService: Option[String]): Boolean = {
    val bucketCriterion  = bucket >= bucketFrom && bucket < bucketTo
    val serviceCriterion = servicesLimit match {
      case None                  => true
      case Some(allowedServices) =>
        maybeService match {
          case Some(service) => allowedServices.contains(service)
          case None          => false
        }
    }
    bucketCriterion && serviceCriterion
  }
}

object FeatureEnablingRule {

  private def parseStatements(input: String) = {
    val Statement = "(\\w+)=(.+)".r

    input
      .split(";")
      .map {
        case Statement(key, value) => (key, value)
        case invalid               => throw new Exception(s"Unparseable feature enabling rule '$invalid'")
      }
      .toMap
  }

  def parse(input: String): FeatureEnablingRule = {

    val statements = parseStatements(input)

    val feature: Feature = statements
      .get("feature")
      .flatMap(Feature.byName.lift)
      .getOrElse(throw new Exception(s"Cannot find valid 'feature' field in rule '$input'"))

    val bucketFrom: Int = statements
      .get("bucketFrom")
      .flatMap(value => Try(Integer.parseInt(value)).toOption)
      .getOrElse(throw new Exception(s"Cannot find valid 'bucketFrom' field in rule '$input'"))

    val bucketTo: Int = statements
      .get("bucketTo")
      .flatMap(value => Try(Integer.parseInt(value)).toOption)
      .getOrElse(throw new Exception(s"Cannot find valid 'bucketTo' field in rule '$input'"))

    val services: Option[Set[String]] = statements.get("services").map(_.split(",").toSet)

    FeatureEnablingRule(bucketFrom, bucketTo, services, feature)
  }
}

class BucketBasedFeatureSelector(computeBucket: BucketCalculator, rules: Iterable[FeatureEnablingRule])
    extends FeatureSelector
    with Logging {

  override def computeFeatures(request: Request[_], service: Option[String]): Set[Feature] = {
    val bucket   = computeBucket(request)
    val features = computeFeatures(bucket, service)
    logger.info(s"Request assigned to the bucket $bucket, features enabled: ${features.map(_.name).mkString(";")}")

    features
  }

  private def computeFeatures(bucket: Int, service: Option[String]): Set[Feature] =
    (for (rule <- rules if rule.isApplicable(bucket, service)) yield rule.feature).toSet
}

object BucketCalculator {
  type BucketCalculator = Request[_] => Int

  val deviceIdBucketCalculator: BucketCalculator = { request =>
    val deviceIdMaybe: Option[String] = HeaderCarrierConverter
      .fromHeadersAndSessionAndRequest(request.headers, Some(request.session), Some(request))
      .deviceID
    val deviceId                      = deviceIdMaybe.getOrElse("")
    val bucket                        = math.abs(deviceId.hashCode % 100)
    bucket
  }
}

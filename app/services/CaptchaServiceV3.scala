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

package services

import com.kenshoo.play.metrics.Metrics
import config.AppConfig
import connectors.{CaptchaApiResponseV3, CaptchaConnectorV3, SuccessfulCaptchaApiResponse, UnsuccessfulCaptchaApiResponse}
import javax.inject.Inject
import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait CaptchaService {
  def validateCaptcha(response: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean]
}

class CaptchaServiceV3 @Inject() (captchaConnector: CaptchaConnectorV3, appConfig: AppConfig, metrics: Metrics)(implicit
  ec: ExecutionContext
) extends CaptchaService {

  private val logger = LoggerFactory.getLogger(getClass)

  private lazy val minScore = appConfig.captchaMinScore

  def validateCaptcha(response: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean] =
    captchaConnector
      .verifyResponse(response)
      .map { apiResponse =>
        checkIfBot(apiResponse)
      }
      .recover { case NonFatal(ex) =>
        logger.error("Error checking captcha, letting the form go through", ex)
        false
      }

  private[services] def checkIfBot(apiResponse: CaptchaApiResponseV3): Boolean =
    apiResponse match {
      case SuccessfulCaptchaApiResponse(score, action) =>
        val isBot = scoreWasTooLow(score)
        logger.info(
          s"reCAPTCHA v3 verification for action: $action, " +
            s"score: $score, action: $action, requiredScore: $minScore, isLikelyABot: $isBot"
        )
        metrics.defaultRegistry.histogram("recaptchaScore").update(scoreToPercent(score))
        isBot
      case UnsuccessfulCaptchaApiResponse(errors)      =>
        logger.warn(
          s"reCAPTCHA v3 verification failed, " +
            s"errors: ${errors.mkString(",")}"
        )
        true
    }

  private def scoreToPercent(score: BigDecimal): Int = (score * 100).toInt

  private def scoreWasTooLow(score: BigDecimal): Boolean = score < minScore

}

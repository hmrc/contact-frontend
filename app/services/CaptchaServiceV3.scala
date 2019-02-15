package services

import connectors.{CaptchaApiResponseV3, CaptchaConnectorV3}
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.{Configuration, Logger}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CaptchaServiceV3 @Inject()(captchaConnector: CaptchaConnectorV3, configuration: Configuration)(
  implicit ec: ExecutionContext) {

  def isLikelyABot(response: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean] =
    if (enabled){
      captchaConnector.verifyResponse(response).map { apiResponse =>
        checkIfBot(apiResponse)
      }.recover {
        case NonFatal(ex) =>
          Logger.error("Error checking captcha, letting the form go through", ex)
          false
      }
    } else {
      Future.successful(false)
    }

  private [services] def checkIfBot(apiResponse: CaptchaApiResponseV3): Boolean = {
      import apiResponse._
      val isBot = scoreWasTooLow(apiResponse.score) || requestWasNotAValidReCaptchaToken(apiResponse.success)
      logger.info(s"reCAPTCHA v3 verification for action: $action, " +
      s"score: $score, action: $action, validReCaptchaToken: $success, requiredScore: $minScore, isLikelyABot: $isBot")
      isBot
    }

  private def scoreWasTooLow(score: BigDecimal): Boolean =
    score < minScore

  private def requestWasNotAValidReCaptchaToken(success: Boolean): Boolean = !success

  private val enabled = {
    val key = "captcha.v3.enabled"
    configuration.getBoolean(key).getOrElse(false)
  }

  private val minScore = {
    val key = "captcha.v3.minScore"
    configuration
      .getString(key)
      .map(BigDecimal(_))
      .getOrElse(throw new Exception(s"Cannot find `$key` in configuration"))
  }

  // public because needs to be passed to html, in play 2.6 we could inject directly to view...
  lazy val clientKey: String = {
    val key = "captcha.v3.keys.client"
    configuration
      .getString(key)
      .getOrElse(throw new Exception(s"Cannot find `$key` in configuration"))
  }

  private val logger = LoggerFactory.getLogger(getClass)

}

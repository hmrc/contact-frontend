package connectors

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class CaptchaConnectorV3 @Inject()(http: HttpClient, configuration: Configuration)(implicit ec: ExecutionContext) {

  /**
    * @param userResponseToken - the user response token provided by the reCAPTCHA client-side integration
    */
  def verifyResponse(userResponseToken: String)(implicit hc: HeaderCarrier): Future[CaptchaApiResponseV3] = {
    http.POSTForm[CaptchaApiResponseV3](
      url = verifyUrl,
      body = Map(
        "secret" -> Seq(serverKey),
        "response" -> Seq(userResponseToken)
      )
    )
  }

  /**
    * the shared key between the micro-service and reCAPTCHA.
    */
  private val serverKey: String = {
    val key = "captcha.v3.keys.server"
    configuration
      .getString(key)
      .getOrElse(throw new Exception(s"Cannot find `$key` in configuration"))
  }

  private val verifyUrl = {
    val key = "captcha.v3.verifyUrl"
    configuration
      .getString(key)
      .getOrElse(throw new Exception(s"Cannot find `$key` in configuration"))
  }

}

/** Verification of user's response by reCAPTCHA
  * https://developers.google.com/recaptcha/docs/v3#site-verify-response
  *
  * @param success - whether this request was a valid reCAPTCHA token
  * @param score - the score for this request (0.0 - 1.0)
  * @param action - the action name for this request (identifies e.g. individual form)
  */
final case class CaptchaApiResponseV3(
  success: Boolean,
  score: BigDecimal,
  action: String // todo(konrad) we don't use the action yet, we should investigate and use, it's useful for trackability
)

object CaptchaApiResponseV3 {
  implicit val format: OFormat[CaptchaApiResponseV3] =
    Json.format[CaptchaApiResponseV3]
}

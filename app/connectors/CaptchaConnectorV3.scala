/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package connectors

import config.AppConfig
import javax.inject.Inject
import play.api.libs.json.{Json, _}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class CaptchaConnectorV3 @Inject() (http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def verifyResponse(userResponseToken: String)(implicit hc: HeaderCarrier): Future[CaptchaApiResponseV3] = {

    http
      .POSTForm(
        url = appConfig.captchaVerifyUrl,
        body = Map(
          "secret"   -> Seq(appConfig.captchaServerKey),
          "response" -> Seq(userResponseToken)
        )
      )
      .foreach(resp => println(resp.json))

    import uk.gov.hmrc.http.HttpReads.Implicits._
    http.POSTForm[CaptchaApiResponseV3](
      url = appConfig.captchaVerifyUrl,
      body = Map(
        "secret"   -> Seq(appConfig.captchaServerKey),
        "response" -> Seq(userResponseToken)
      )
    )
  }

}

/** Verification of user's response by reCAPTCHA
  * https://developers.google.com/recaptcha/docs/v3#site-verify-response
  *
  * @param success - whether this request was a valid reCAPTCHA token
  * @param score - the score for this request (0.0 - 1.0)
  * @param action - the action name for this request (identifies e.g. individual form)
  */
sealed trait CaptchaApiResponseV3
case class SuccessfulCaptchaApiResponse(score: BigDecimal, action: String) extends CaptchaApiResponseV3
case class UnsuccessfulCaptchaApiResponse(errorCodes: Seq[String]) extends CaptchaApiResponseV3

object CaptchaApiResponseV3 {

  class ExplicitRuleFormat extends Reads[CaptchaApiResponseV3] {

    val unsuccessfulReads: Reads[UnsuccessfulCaptchaApiResponse] =
      (JsPath \ "error-codes").read[List[String]].map(UnsuccessfulCaptchaApiResponse)

    override def reads(json: JsValue): JsResult[CaptchaApiResponseV3] = {
      val success = (json \ "success").as[Boolean]
      if (success) {
        Json.fromJson[SuccessfulCaptchaApiResponse](json)(Json.reads[SuccessfulCaptchaApiResponse])
      } else {
        Json.fromJson[UnsuccessfulCaptchaApiResponse](json)(unsuccessfulReads)
      }
    }

  }

  implicit val format: Reads[CaptchaApiResponseV3] = new ExplicitRuleFormat()

}

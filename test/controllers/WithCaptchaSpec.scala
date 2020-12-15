/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.AppConfig
import helpers.TestAppConfig
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContent, Request, Result, Results}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import services.CaptchaService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.{ExecutionContext, Future}

class WithCaptchaSpec extends AnyWordSpec with MockitoSugar with Results with Matchers with GuiceOneAppPerSuite {

  implicit val actorSystem: ActorSystem        = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "validateCaptcha" should {

    "pass to the underlying action if not a bot" in {
      val testController = new TestController
      val fields         = Map(
        "recaptcha-v3-response" -> "notABot",
        "other-field"           -> "xxx"
      )

      val contactRequest         = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result: Future[Result] = testController.action(contactRequest)

      status(result)          shouldBe 200
      contentAsString(result) shouldBe "not a bot"
    }

    "fail with bad request if required fields not found in the request" in {
      val testController = new TestController
      val fields         = Map(
        "other-field" -> "xxx"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = testController.action(contactRequest)

      status(result)        shouldBe 400
      contentAsString(result) should include("deskpro.error.problem.sending")
    }

    "fail with bad request if detected as a bot" in {
      val testController = new TestController
      val fields         = Map(
        "recaptcha-v3-response" -> "isAbot",
        "other-field"           -> "xxx"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = testController.action(contactRequest)

      status(result)        shouldBe 400
      contentAsString(result) should include("deskpro.error.problem.sending")
    }

    class TestController(enabled: Boolean = true)
        extends WithCaptcha(
          Stubs.stubMessagesControllerComponents(messagesApi = Helpers.stubMessagesApi()),
          app.injector.instanceOf[views.html.DeskproErrorPage],
          app.injector.instanceOf[views.html.helpers.recaptcha]
        ) {
      override implicit val appConfig: AppConfig = new TestAppConfig {
        override def captchaEnabled: Boolean = enabled
      }
      override implicit val captchaService       = new CaptchaService {
        override def validateCaptcha(response: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean] =
          Future.successful(response == "notABot")
      }

      override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

      def action(request: Request[AnyContent]) =
        validateCaptcha(request) {
          Future(Ok("not a bot"))
        }
    }

  }

}

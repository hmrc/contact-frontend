package unit.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.AppConfig
import controllers.WithCaptcha
import org.scalatest.GivenWhenThen
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, MessagesApi}
import play.api.mvc.{AnyContent, Request, Results}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import services.CaptchaService
import support.util.TestAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class WithCaptchaSpec extends UnitSpec
  with GivenWhenThen
  with MockitoSugar
  with Results
  with WithFakeApplication {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: akka.util.Timeout = 10.seconds

  "validateCaptcha" should {

    "pass to the underlying action if not a bot" in {
      val testController = new TestController
      val fields = Map(
        "recaptcha-v3-response" -> "notABot",
        "other-field" -> "xxx"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      val result = await(testController.action(contactRequest))

      status(result) shouldBe 200
      bodyOf(result) shouldBe "not a bot"

    }

    "fail with bad request if required fields not found in the request" in {
      val testController = new TestController
      val fields = Map(
        "other-field" -> "xxx"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)


      val result = await(testController.action(contactRequest))

      status(result) shouldBe 400
      bodyOf(result) should include("There was a problem sending your query")
    }

    "fail with bad request if detected as a bot" in {
      val testController = new TestController
      val fields = Map(
        "recaptcha-v3-response" -> "isAbot",
        "other-field" -> "xxx"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      val result = await(testController.action(contactRequest))

      status(result) shouldBe 400
      bodyOf(result) should include("There was a problem sending your query")
    }

    "succeed if required fields not found but captcha not enabled"

    class TestController(enabled : Boolean = true) extends WithCaptcha {
      override implicit val appConfig: AppConfig = new TestAppConfig {
        override def captchaEnabled: Boolean = enabled
      }
      override implicit val captchaService = new CaptchaService {
        override def validateCaptcha(response: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean] =
          Future.successful(response == "notABot")
      }

      val env = Environment.simple()
      val configuration = Configuration.load(env)

      override def messagesApi: MessagesApi = new DefaultMessagesApi(env, configuration, new DefaultLangs(configuration))

      def action(request : Request[AnyContent]) = {
        validateCaptcha(request) {
          Future(Ok("not a bot"))
        }
      }
    }

  }




}

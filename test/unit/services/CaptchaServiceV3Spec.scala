package services

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import connectors.{CaptchaApiResponseV3, CaptchaConnectorV3, SuccessfulCaptchaApiResponse, UnsuccessfulCaptchaApiResponse}
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import support.util.TestAppConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class CaptchaServiceV3Spec extends WordSpec with Matchers with MockitoSugar with PropertyChecks with ScalaFutures {

  "Verifying user's response" should {

    "classify as bot if score < min required score" in new Fixtures {
      forAll(scoreGen, scoreGen) { (minScoreRequired, actualScore) =>
        val isBot = service(minScoreRequired).checkIfBot(SuccessfulCaptchaApiResponse(actualScore, "action"))
        isBot shouldBe (actualScore < minScoreRequired)
      }
    }

    "classify as bot if response was not a valid reCAPTCHA token" in new Fixtures {
      val minScore = 0.5
      val actualScore = 0.6

      val isBot = service(minScore).checkIfBot(UnsuccessfulCaptchaApiResponse(Seq("error")))

      isBot shouldBe true
    }

    "add the result to metrics" in new Fixtures {
      forAll(scoreGen, scoreGen) { (minScoreRequired, actualScore) =>

        metricsStub.defaultRegistry.remove("recaptchaScore")

        service(minScoreRequired).checkIfBot(SuccessfulCaptchaApiResponse(actualScore, "action"))

        metricsStub.defaultRegistry.histogram("recaptchaScore").getSnapshot.getValues.shouldBe(Array((actualScore * 100).toLong))
      }
    }

  }

  trait Fixtures {
    implicit val hc = HeaderCarrier()

    lazy val metricsStub = new Metrics {
      override lazy val defaultRegistry: MetricRegistry = new MetricRegistry

      override def toJson: String = ???
    }

    val scoreGen: Gen[BigDecimal] =
          Gen.chooseNum[Double](0,1).map(BigDecimal(_))

    def service(minScore: BigDecimal = 0.5) = new CaptchaServiceV3(
      mock[CaptchaConnectorV3],
      new TestAppConfig {
        override def captchaMinScore: BigDecimal = minScore
      }, metricsStub)(ExecutionContext.global)

  }


}

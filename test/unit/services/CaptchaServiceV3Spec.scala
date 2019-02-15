package services

import connectors.{CaptchaApiResponseV3, CaptchaConnectorV3}
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class CaptchaServiceV3Spec extends WordSpec with Matchers with MockitoSugar with PropertyChecks with ScalaFutures {

  "Verifying user's response" should {

    "classify as bot if score < min required score" in new Fixtures {
      forAll(scoreGen, scoreGen) { (minScoreRequired, actualScore) =>
        val isBot = service(minScoreRequired).checkIfBot(CaptchaApiResponseV3(success = true, actualScore, "action"))
        isBot shouldBe (actualScore < minScoreRequired)
      }
    }

    "classify as bot if response was not a valid reCAPTCHA token" in new Fixtures {
      val minScore = 0.5
      val actualScore = 0.6

      val isBot = service(minScore).checkIfBot(CaptchaApiResponseV3(success = false, actualScore, "action"))

      isBot shouldBe true
    }

    "take 'action' into account" in new Fixtures {
      pending
      fail("Verify that action belongs to a particular form")
    }

  }

  trait Fixtures {
    implicit val hc = HeaderCarrier()

    val scoreGen: Gen[BigDecimal] =
      Gen.chooseNum[Double](0,1).map(BigDecimal(_))

    def service(minScore: BigDecimal = 0.5) = new CaptchaServiceV3(
      mock[CaptchaConnectorV3],
      Configuration("captcha.v3.minScore" -> minScore)
    )(ExecutionContext.global)
  }


}

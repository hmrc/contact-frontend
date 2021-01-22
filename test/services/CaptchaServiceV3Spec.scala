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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import connectors.{CaptchaConnectorV3, SuccessfulCaptchaApiResponse, UnsuccessfulCaptchaApiResponse}
import helpers.TestAppConfig
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class CaptchaServiceV3Spec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with ScalaFutures {

  "Verifying user's response" should {

    "classify as bot if score < min required score" in new Fixtures {
      forAll(scoreGen, scoreGen) { (minScoreRequired, actualScore) =>
        val isBot = service(minScoreRequired).checkIfBot(SuccessfulCaptchaApiResponse(actualScore, "action"))
        isBot shouldBe (actualScore < minScoreRequired)
      }
    }

    "classify as bot if response was not a valid reCAPTCHA token" in new Fixtures {
      val minScore = 0.5

      val isBot = service(minScore).checkIfBot(UnsuccessfulCaptchaApiResponse(Seq("error")))

      isBot shouldBe true
    }

    "add the result to metrics" in new Fixtures {
      forAll(scoreGen, scoreGen) { (minScoreRequired, actualScore) =>
        metricsStub.defaultRegistry.remove("recaptchaScore")

        service(minScoreRequired).checkIfBot(SuccessfulCaptchaApiResponse(actualScore, "action"))

        metricsStub.defaultRegistry
          .histogram("recaptchaScore")
          .getSnapshot
          .getValues
          .shouldBe(Array((actualScore * 100).toLong))
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
      Gen.chooseNum[Double](0, 1).map(BigDecimal(_))

    def service(minScore: BigDecimal = 0.5) = new CaptchaServiceV3(
      mock[CaptchaConnectorV3],
      new TestAppConfig {
        override def captchaMinScore: BigDecimal = minScore
      },
      metricsStub
    )(ExecutionContext.global)

  }

}

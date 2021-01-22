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

package controllers

import config.CFConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Configuration}
import play.api.data.FormError
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class AssetsFrontendSurveyControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"                  -> false,
        "metrics.enabled"              -> false,
        "enablePlayFrontendSurveyForm" -> false
      )
      .build()

  "ticketId regex" should {
    "validates correct ticket refs" in new TestScope {
      controller.validateTicketId("GFBN-8051-KLNY") shouldBe true
    }

    "rejects invalid ticket refs" in new TestScope {
      controller.validateTicketId("GFBN-8051-") shouldBe false
      controller.validateTicketId("")           shouldBe false
    }
  }

  "SurveyController" should {
    "produce an audit result for a valid form" in new TestScope {
      implicit val request = FakeRequest(
        "GET",
        "/blah",
        FakeHeaders(),
        Map[String, Seq[String]](
          "helpful"    -> Seq("2"),
          "ticket-id"  -> Seq("GFBN-8051-KLNY"),
          "service-id" -> Seq("abcdefg")
        )
      )

      val audit: Either[Option[Future[DataEvent]], Seq[FormError]] = controller.getAuditEventOrFormErrors
      val dataFuture: Future[DataEvent]                            = audit match {
        case Left(success) => success.getOrElse(Future.failed(new Exception("No data event")))
        case Right(errors) => Future.failed(new Exception(errors.mkString(", ")))
      }
      val data                                                     = Await.result(dataFuture, 5.seconds)

      data.auditSource shouldBe "frontend"
      data.auditType   shouldBe "DeskproSurvey"
      data.detail      shouldBe Map[String, String](
        "helpful"   -> "2",
        "speed"     -> "0",
        "improve"   -> "",
        "ticketId"  -> "GFBN-8051-KLNY",
        "serviceId" -> "abcdefg"
      )
    }

    "product errors for an invalid form" in new TestScope {
      implicit val request = FakeRequest(
        "GET",
        "/blah",
        FakeHeaders(),
        Map[String, Seq[String]](
          "helpful"    -> Seq("2"),
          "ticket-id"  -> Seq("GFBN-8051-KLNY"),
          "service-id" -> Seq("abcdefg122445345345dfddfds234234sdf")
        )
      )

      val audit  = controller.getAuditEventOrFormErrors
      val errors = audit.getOrElse(Nil)

      errors.size                     shouldBe 1
      errors.headOption map { _.key } shouldBe Some("service-id")
    }
  }

  class TestScope extends MockitoSugar {
    val controller = new SurveyController(
      mock[AuditConnector],
      Stubs.stubMessagesControllerComponents(),
      mock[views.html.survey],
      mock[views.html.SurveyPage],
      mock[views.html.survey_confirmation],
      mock[views.html.SurveyConfirmationPage]
    )(new CFConfig(Configuration()), ExecutionContext.Implicits.global)
  }
}

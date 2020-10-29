/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.CFConfig
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.data.FormError
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class SurveyControllerSpec extends AnyWordSpec with Matchers {

  "ticketId regex" should {
    "validates correct ticket refs" in new SurveyControllerApplication {
      controller.validateTicketId("GFBN-8051-KLNY") shouldBe true
    }

    "rejects invalid ticket refs" in new SurveyControllerApplication {
      controller.validateTicketId("GFBN-8051-") shouldBe false
      controller.validateTicketId("")           shouldBe false
    }
  }

  "SurveyController" should {
    "produce an audit result for a valid form" in new SurveyControllerApplication {
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

    "product errors for an invalid form" in new SurveyControllerApplication {
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

}

class SurveyControllerApplication extends MockitoSugar {

  val controller = new SurveyController(
    mock[AuditConnector],
    Stubs.stubMessagesControllerComponents(),
    mock[views.html.survey],
    mock[views.html.survey_confirmation]
  )(new CFConfig(Configuration()), ExecutionContext.Implicits.global)
}

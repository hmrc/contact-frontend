/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import config.CFConfig
import helpers.JsoupHelpers
import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import play.api.test.Helpers._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PlayFrontendSurveyControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerTest with JsoupHelpers {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"                  -> false,
        "metrics.enabled"              -> false,
        "enablePlayFrontendSurveyForm" -> true
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

  "display errors when form isn't filled out at all" in new TestScope {
    val fields  = Map(
      "ticket-id"  -> "GFBN-8051-KLNY",
      "service-id" -> "abcdefg"
    )
    val request = FakeRequest("POST", "/blah")

    implicit val messages = messagesApi.preferred(request)
    val result            = controller.submit()(request)

    status(result) should be(400)

    val document = Jsoup.parse(contentAsString(result))
    val errors   = document.select(".govuk-error-message")

    errors               should have size 2
    errors.get(0).text() should include(messages("survey.helpful.error.required"))
    errors.get(1).text() should include(messages("survey.speed.error.required"))
  }

  "display errors when comment is too long" in new TestScope {
    val fields  = Map(
      "helpful"    -> "1",
      "speed"      -> "1",
      "improve"    -> "x" * 3000,
      "ticket-id"  -> "GFBN-8051-KLNY",
      "service-id" -> "abcdefg"
    )
    val request = FakeRequest("POST", "/blah").withFormUrlEncodedBody(fields.toSeq: _*)

    implicit val messages = messagesApi.preferred(request)
    val result            = controller.submit()(request)

    status(result) should be(400)

    val document = Jsoup.parse(contentAsString(result))
    val errors   = document.select(".govuk-error-message")

    errors               should have size 1
    errors.get(0).text() should include(messages("survey.improve.error.length"))
  }

  class TestScope extends MockitoSugar {
    val playFrontendSurveyPage = app.injector.instanceOf[views.html.SurveyPage]
    val cconfig: CFConfig      = new CFConfig(app.configuration)
    val messagesApi            = app.injector.instanceOf[MessagesApi]

    val controller = new SurveyController(
      mock[AuditConnector],
      Stubs.stubMessagesControllerComponents(messagesApi = messagesApi),
      mock[views.html.survey],
      playFrontendSurveyPage,
      mock[views.html.survey_confirmation],
      mock[views.html.SurveyConfirmationPage]
    )(cconfig, ExecutionContext.Implicits.global)
  }
}

/*
 * Copyright 2023 HM Revenue & Customs
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

import helpers.{BaseControllerSpec, JsoupHelpers}
import org.jsoup.Jsoup
import play.api.data.FormError
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.Helpers.*
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class SurveyControllerSpec extends BaseControllerSpec with JsoupHelpers {

  val notFoundPage           = instanceOf[views.html.NotFoundPage]
  val playFrontendSurveyPage = instanceOf[views.html.SurveyPage]
  val messagesApi            = instanceOf[MessagesApi]

  val controller = new SurveyController(
    mock[AuditConnector],
    Stubs.stubMessagesControllerComponents(messagesApi = messagesApi),
    notFoundPage,
    playFrontendSurveyPage,
    mock[views.html.SurveyConfirmationPage]
  )

  "SurveyController" should {
    "bind query string parameters to the form as expected" in {
      val ticketId  = "GFBN-8051-KLNY"
      val serviceId = "abcdefg"

      val result   = controller.survey(ticketId, serviceId)(FakeRequest())
      val document = Jsoup.parse(contentAsString(result))

      val queryString = s"ticketId=$ticketId&serviceId=$serviceId"
      document.body().select("form[id=survey-form]").first.attr("action") shouldBe s"/contact/survey?$queryString"

      document.body().select("input[name=service-id]").first.attr("value") shouldBe serviceId
      document.body().select("input[name=ticket-id]").first.attr("value")  shouldBe ticketId
    }

    "produce an audit result for a valid form" in {
      given FakeRequest[?] = FakeRequest(
        "POST",
        "/",
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

    "produce errors for an invalid form" in {
      given FakeRequest[?] = FakeRequest(
        "POST",
        "/",
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

    "return a page not found error page when ticketId is invalid" in {
      val ticketId  = "ABCD-EFGH-"
      val serviceId = "abcdefg"

      val result   = controller.survey(ticketId, serviceId)(FakeRequest())
      val document = Jsoup.parse(contentAsString(result))

      status(result)                        should be(404)
      document.body().select("h1").text() shouldBe "Page not found"
    }
  }

  "display errors when form isn't filled out at all" in {
    val ticketId  = "GFBN-8051-KLNY"
    val serviceId = "abcdefg"
    val request   = FakeRequest("POST", "/")

    given messages: Messages = messagesApi.preferred(request)

    val result = controller.submit(ticketId, serviceId)(request)

    status(result) should be(400)

    val document = Jsoup.parse(contentAsString(result))

    val queryString = s"ticketId=$ticketId&serviceId=$serviceId"
    document.body().select("form[id=survey-form]").first.attr("action") shouldBe s"/contact/survey?$queryString"

    val errors = document.select(".govuk-error-message")

    errors               should have size 2
    errors.get(0).text() should include(messages("survey.helpful.error.required"))
    errors.get(1).text() should include(messages("survey.speed.error.required"))
  }

  "display errors when comment is too long" in {
    val ticketId  = "GFBN-8051-KLNY"
    val serviceId = "abcdefg"
    val fields    = Map(
      "helpful"    -> "1",
      "speed"      -> "1",
      "improve"    -> "x" * 3000,
      "ticket-id"  -> ticketId,
      "service-id" -> serviceId
    )
    val request   = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)

    given messages: Messages = messagesApi.preferred(request)

    val result = controller.submit(ticketId, serviceId)(request)

    status(result) should be(400)

    val document = Jsoup.parse(contentAsString(result))

    val queryString = s"ticketId=$ticketId&serviceId=$serviceId"
    document.body().select("form[id=survey-form]").first.attr("action") shouldBe s"/contact/survey?$queryString"

    val errors = document.select(".govuk-error-message")

    errors               should have size 1
    errors.get(0).text() should include(messages("survey.improve.error.length"))
  }
}

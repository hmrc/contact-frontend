/*
 * Copyright 2025 HM Revenue & Customs
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

import config.*
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.TicketId
import controllers.testOnly.ReportOneLoginProblemController
import helpers.ApplicationSupport
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.{ExecutionContext, Future}

class ReportOneLoginProblemControllerSpec extends AnyWordSpec with ApplicationSupport with Matchers {

  given Messages =
    app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  "Requesting the standalone page with endpoints disabled" should {

    "return Not Found and error HTML for index" in new TestScope {
      val controller = setupController(false, Future.successful(TicketId(12345)))
      val result     = controller.index()(FakeRequest())

      status(result) should be(NOT_FOUND)

      val document: Document = Jsoup.parse(contentAsString(result))
      val header: Element    = document.getElementsByClass("govuk-heading-xl").first

      header          should not be null
      header.text() shouldBe "Sorry, there is a problem with the service"
    }

    "return Not Found and error HTML for thanks" in new TestScope {
      val controller = setupController(false, Future.successful(TicketId(12345)))
      val result     = controller.thanks()(FakeRequest())

      status(result) should be(NOT_FOUND)

      val document: Document = Jsoup.parse(contentAsString(result))
      val header: Element    = document.getElementsByClass("govuk-heading-xl").first

      header          should not be null
      header.text() shouldBe "Sorry, there is a problem with the service"
    }

    "return Not Found and error HTML for submit" in new TestScope {
      val controller = setupController(false, Future.successful(TicketId(12345)))
      val result     = controller.submit()(generateRequest())

      status(result) should be(NOT_FOUND)

      val document: Document = Jsoup.parse(contentAsString(result))
      val header: Element    = document.getElementsByClass("govuk-heading-xl").first

      header          should not be null
      header.text() shouldBe "Sorry, there is a problem with the service"
    }
  }

  "Requesting the standalone page with endpoints enabled" should {
    "return OK and valid HTML" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))
      val result     = controller.index()(FakeRequest())
      status(result) should be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))

      document.getElementById("one-login-complaint-form")       should not be null
      document.getElementsByClass("govuk-error-summary").size() should be(0)
    }
  }

  "Reporting a problem via the standalone page with endpoints enabled" should {
    "redirect to a Thank You html page for a valid request" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))

      val request = generateRequest()
      val result  = controller.submit()(request)

      status(result)             should be(SEE_OTHER)
      redirectLocation(result) shouldBe Some("/contact/test-only/report-one-login-complaint/thanks")
    }

    "return Bad Request and page with validation error for invalid input" in new TestScope {
      val controller     = setupController(true, Future.successful(TicketId(12345)))
      val invalidRequest = FakeRequest("POST", "/").withFormUrlEncodedBody("some-key" -> "some-value")
      val result         = controller.submit()(invalidRequest)

      status(result) should be(BAD_REQUEST)
      verifyNoInteractions(controller.ticketQueueConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
      document
        .body()
        .select("form[id=one-login-complaint-form]")
        .first
        .attr("action")                                       shouldBe s"/contact/test-only/report-one-login-complaint"
    }

    "return Bad Request and page with validation error if the name has invalid characters" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))
      val request    = generateRequest(
        name = """<a href="blah.com">something</a>"""
      )

      val submit = controller.submit()(request)
      val page   = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(controller.ticketQueueConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request and page with validation error if the email has invalid syntax (for Deskpro)" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))
      val request    = generateRequest(email = "a.a.a")
      val submit     = controller.submit()(request)
      val page       = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(controller.ticketQueueConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request and page with validation error if the NINO format is invalid" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))

      val request = generateRequest(nino = "I don't know")
      val submit  = controller.submit()(request)
      val page    = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(controller.ticketQueueConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request and page with validation error if the SA UTR format is invalid" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))
      val request    = generateRequest(saUtr = Some("Invalid number"))
      val submit     = controller.submit()(request)
      val page       = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(controller.ticketQueueConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Internal Server Error and error page if the Deskpro ticket creation fails" in new TestScope {
      val controller = setupController(true, Future.failed(Exception("Expected connector exception")))

      val result = controller.submit()(generateRequest())
      status(result) should be(INTERNAL_SERVER_ERROR)

      val document = Jsoup.parse(contentAsString(result))
      document.text() should include("Try again later.")
    }
  }

  "Requesting the standalone thanks page" should {
    "return OK and valid html" in new TestScope {
      val controller = setupController(true, Future.successful(TicketId(12345)))
      val result     = controller.thanks()(FakeRequest())

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be(0)
      document.getElementsByClass("govuk-panel__title").text()  should be(
        "We have received your One Login for Government complaint"
      )
    }
  }

  class TestScope extends MockitoSugar {

    def setupController(
      enableEndpoints: Boolean,
      connectorResponse: Future[TicketId]
    ): ReportOneLoginProblemController = {
      val reportProblemPage = app.injector.instanceOf[views.html.ReportOneLoginProblemPage]
      val confirmationPage  = app.injector.instanceOf[views.html.ReportOneLoginProblemConfirmationPage]
      val errorPage         = app.injector.instanceOf[views.html.InternalErrorPage]

      given ExecutionContext = ExecutionContext.global
      given HeaderCarrier    = any[HeaderCarrier]

      given cfconfig: AppConfig = new CFConfig(app.configuration) {
        override def enableOlfgComplaintsEndpoints: Boolean = enableEndpoints
      }

      val mockConnector = mock[DeskproTicketQueueConnector]
      when(
        mockConnector.createDeskProTicket(
          any,
          any,
          any,
          any,
          any,
          any,
          any,
          any,
          any,
          any
        )
      ).thenReturn(connectorResponse)

      new ReportOneLoginProblemController(
        mockConnector,
        Stubs.stubMessagesControllerComponents(messagesApi = app.injector.instanceOf[MessagesApi]),
        reportProblemPage,
        confirmationPage,
        errorPage
      )
    }

    val deskproName: String    = "Gary Grapefruit"
    val deskproEmail: String   = "grapefruit@test.com"
    val deskproSubject: String = "Support Request"
    val deskproNino: String    = "AA112233B"
    given Messages             = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

    def generateRequest(
      name: String = deskproName,
      email: String = deskproEmail,
      nino: String = deskproNino,
      saUtr: Option[String] = None
    ): FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "name"                -> name,
          "nino"                -> nino,
          "sa-utr"              -> saUtr.getOrElse(""),
          "date-of-birth.day"   -> "10",
          "date-of-birth.month" -> "10",
          "date-of-birth.year"  -> "1990",
          "email"               -> email,
          "phone-number"        -> "07711 112233",
          "address"             -> "1 The Street, London, SW1A",
          "contact-preference"  -> "email",
          "complaint"           -> "This is a complaint",
          "csrfToken"           -> "token"
        )
  }
}

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

package controllers.testOnly

import config.*
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.{TicketConstants, TicketId}
import helpers.ApplicationSupport
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.{ExecutionContext, Future}

class ReportOneLoginProblemControllerSpec extends AnyWordSpec with ApplicationSupport with Matchers {

  given Messages =
    app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  "Requesting the standalone page" should {
    "return OK and valid HTML" in new TestScope {
      val result = controller.index()(FakeRequest())

      status(result) should be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))

      document.getElementById("error-feedback-form")            should not be null
      document.getElementsByClass("govuk-error-summary").size() should be(0)
    }
  }

  "Reporting a problem via the standalone page" should {
    "redirect to a Thank You html page for a valid request" in new TestScope {
      hrmcConnectorWillReturnTheTicketId

      val request = generateRequest()
      val result  = controller.submit()(request)

      status(result)             should be(SEE_OTHER)
      redirectLocation(result) shouldBe Some("/contact/test-only/report-one-login-problem/thanks")
    }

    "return Bad Request and page with validation error for invalid input" in new TestScope {
      val result = controller.submit()(generateInvalidRequest())

      status(result) should be(BAD_REQUEST)
      verifyNoInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request, page with validation error, and correct submit URL if no service URL param for invalid input" in new TestScope {
      val headers = Seq((REFERER, deskproReferrer), ("User-Agent", "iAmAUserAgent"))
      val request = FakeRequest("POST", "/")
        .withHeaders(headers: _*)
        .withFormUrlEncodedBody(
          "report-name"     -> deskproName,
          "THIS-WILL-ERROR" -> deskproEmail,
          "report-action"   -> "Some Action",
          "report-error"    -> "Some Error",
          "isJavascript"    -> "false",
          "service"         -> ""
        )

      val result = controller.submit()(request)

      status(result) should be(BAD_REQUEST)
      verifyNoInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
      document
        .body()
        .select("form[id=error-feedback-form]")
        .first
        .attr("action")                                       shouldBe s"/contact/test-only/report-one-login-problem"
    }

    "return Bad Request and page with validation error if the name has invalid characters" in new TestScope {
      val request = generateRequest(
        name = """<a href="blah.com">something</a>"""
      )

      val submit = controller.submit()(request)
      val page   = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request and page with validation error if the email has invalid syntax (for Deskpro)" in new TestScope {
      val request = generateRequest(email = "a.a.a")
      val submit  = controller.submit()(request)
      val page    = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

//    "return Internal Server Error and error page if the Deskpro ticket creation fails" in new TestScope {
//      when(
//        hmrcDeskproConnector.createDeskProTicket(
//          meq("John Densmore"),
//          meq("name@mail.com"),
//          meq(controller.problemMessage("Some Action", "Some Error")),
//          meq("/contact/problem_reports"),
//          meq(false),
//          any[Request[AnyRef]](),
//          meq(None),
//          meq(Some("one-login-complaint")),
//          meq(None),
//          any[TicketConstants]
//        )
//      ).thenReturn(Future.failed(new Exception("failed")))
//
//      val request = generateRequest(isAjaxRequest = false)
//      val result  = controller.submit()(request)
//      status(result) should be(INTERNAL_SERVER_ERROR)
//
//      val document = Jsoup.parse(contentAsString(result))
//      document.text() should include("Try again later.")
//    }
  }

  "Requesting the standalone thanks page" should {
    "return OK and valid html" in new TestScope {
      val result = controller.thanks()(FakeRequest())

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be(0)
      document.getElementsByClass("govuk-panel__title").text()  should be(
        "We have received your technical problem report"
      )
    }
  }

  class TestScope extends MockitoSugar {

    val reportProblemPage = app.injector.instanceOf[views.html.testOnly.ReportOneLoginProblemPage]
    val confirmationPage  = app.injector.instanceOf[views.html.testOnly.ReportOneLoginProblemConfirmationPage]
    val errorPage         = app.injector.instanceOf[views.html.InternalErrorPage]

    given cfconfig: AppConfig = new CFConfig(app.configuration)
    given ExecutionContext    = ExecutionContext.global
    given HeaderCarrier       = any[HeaderCarrier]

    val controller = new ReportOneLoginProblemController(
      mock[DeskproTicketQueueConnector],
      Stubs.stubMessagesControllerComponents(messagesApi = app.injector.instanceOf[MessagesApi]),
      reportProblemPage,
      confirmationPage,
      errorPage
    )

    val deskproName: String           = "John Densmore"
    val deskproEmail: String          = "name@mail.com"
    val deskproSubject: String        = "Support Request"
    given Messages                    = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))
    val deskproProblemMessage: String =
      controller.problemMessage("Some Action", "Some Error")
    val deskproReferrer: String       = "/contact/problem_reports"

    val hmrcDeskproConnector = controller.ticketQueueConnector

    val enrolments = Some(Enrolments(Set()))

    def generateRequest(
      name: String = deskproName,
      email: String = deskproEmail
    ): FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "name"                -> name,
          "nino"                -> "AB112233B",
          "saUtr"               -> "1234567890",
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

    def generateInvalidRequest() =
      FakeRequest("POST", "/")
        .withFormUrlEncodedBody("some-key" -> "some-value")

    def hmrcConnectorWillFail =
      mockHmrcConnector(Future.failed(new Exception("failed")))

    def hrmcConnectorWillReturnTheTicketId =
      mockHmrcConnector(Future.successful(TicketId(123)))

    private def mockHmrcConnector(result: Future[TicketId]) =
      when(
        hmrcDeskproConnector.createDeskProTicket(
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
      ).thenReturn(result)

//    private def mockHmrcConnector(result: Future[TicketId]) =
//      when(
//        hmrcDeskproConnector.createDeskProTicket(
//          meq(deskproName),
//          meq(deskproEmail),
//          meq(deskproProblemMessage),
//          meq(deskproReferrer),
//          meq(false),
//          any[Request[AnyRef]](),
//          meq(None),
//          meq(Some("one-login-complaint")),
//          meq(None),
//          any[TicketConstants]
//        )
//      ).thenReturn(result)
  }

}

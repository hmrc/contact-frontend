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

import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.{TicketConstants, TicketId}
import connectors.enrolments.EnrolmentsConnector
import helpers.BaseControllerSpec
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.*
import org.mockito.stubbing.OngoingStubbing
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.RefererHeaderRetriever

import scala.concurrent.{ExecutionContext, Future}

class ReportProblemControllerSpec extends BaseControllerSpec {

  val enrolmentsConnector: EnrolmentsConnector = mock[EnrolmentsConnector]
  when(enrolmentsConnector.maybeAuthenticatedUserEnrolments()(using any())(using any()))
    .thenReturn(Future.successful(None))

  val hmrcDeskproConnector: DeskproTicketQueueConnector = mock[DeskproTicketQueueConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(hmrcDeskproConnector)
  }

  val reportProblemPage = instanceOf[views.html.ReportProblemPage]
  val confirmationPage  = instanceOf[views.html.ReportProblemConfirmationPage]
  val errorPage         = instanceOf[views.html.InternalErrorPage]

  val controller = new ReportProblemController(
    hmrcDeskproConnector,
    enrolmentsConnector,
    Stubs.stubMessagesControllerComponents(messagesApi = app.injector.instanceOf[MessagesApi]),
    reportProblemPage,
    confirmationPage,
    errorPage,
    new RefererHeaderRetriever
  )

  val deskproName: String           = "John Densmore"
  val deskproEmail: String          = "name@mail.com"
  val deskproSubject: String        = "Support Request"
  val deskproProblemMessage: String = controller.problemMessage("Some Action", "Some Error")
  val deskproReferrer: String       = "/contact/problem_reports"

  val enrolments = Some(Enrolments(Set()))

  def generateRequest(
    isAjaxRequest: Boolean,
    name: String = deskproName,
    email: String = deskproEmail
  ) = {

    val headers = Seq(
      (REFERER, deskproReferrer),
      ("User-Agent", "iAmAUserAgent")
    ) ++ Seq(
      ("X-Requested-With", "XMLHttpRequest")
    ).filter(_ => isAjaxRequest)

    FakeRequest("POST", "/")
      .withHeaders(headers: _*)
      .withFormUrlEncodedBody(
        "report-name"   -> name,
        "report-email"  -> email,
        "report-action" -> "Some Action",
        "report-error"  -> "Some Error",
        "csrfToken"     -> "token",
        "isJavascript"  -> isAjaxRequest.toString
      )
  }

  def generateInvalidRequest(isAjaxRequest: Boolean) = {

    val headers = Seq(
      (REFERER, deskproReferrer),
      ("User-Agent", "iAmAUserAgent")
    ) ++ Seq(
      ("X-Requested-With", "XMLHttpRequest")
    ).filter(_ => isAjaxRequest)

    FakeRequest("POST", "/")
      .withHeaders(headers: _*)
      .withFormUrlEncodedBody("isJavascript" -> isAjaxRequest.toString)
  }

  def setHmrcConnectorResponse(result: Future[TicketId]): OngoingStubbing[Future[TicketId]] =
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
      )(using any[HeaderCarrier])
    ).thenReturn(result)

  "Requesting the standalone page" should {
    "return OK and valid HTML" in {
      val result = controller.index(Some("my-test-service"), Some("my-referrer-url"))(FakeRequest())

      status(result) should be(OK)

      val document    = Jsoup.parse(contentAsString(result))
      val queryString = s"service=my-test-service&referrerUrl=my-referrer-url"
      document
        .body()
        .select("form[id=error-feedback-form]")
        .first
        .attr("action") shouldBe s"/contact/report-technical-problem?$queryString"

      document.getElementById("error-feedback-form")            should not be null
      document.getElementsByClass("govuk-error-summary").size() should be(0)
    }

    "bind the referrer from the URL rather than headers if both provided" in {
      val requestWithHeaders = FakeRequest().withHeaders((REFERER, "referrer-from-header"))
      val result             = controller.index(Some("my-test-service"), Some("referrer-from-url"))(requestWithHeaders)

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=referrer]").`val` should be("referrer-from-url")
    }

    "bind the referrer from the header if no URL parameter passed in" in {
      val requestWithHeaders = FakeRequest().withHeaders((REFERER, "referrer-from-header"))
      val result             = controller.index(Some("my-test-service"), None)(requestWithHeaders)

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=referrer]").`val` should be("referrer-from-header")
    }
  }

  "Requesting the deprecated standalone page" should {
    "redirect to the non-deprecated page with the REFERER passed via the URL" in {
      val requestWithHeaders = FakeRequest().withHeaders((REFERER, "referrer-from-header"))
      val result             = controller.indexDeprecated(Some("my-test-service"), Some("referrer-from-url"))(requestWithHeaders)

      status(result) should be(SEE_OTHER)
      val queryString = s"service=my-test-service&referrerUrl=referrer-from-url"
      redirectLocation(result) should be(Some(s"/contact/report-technical-problem?$queryString"))
    }

    "redirect to the non-deprecated page with the REFERER passed via the header" in {
      val requestWithHeaders = FakeRequest().withHeaders((REFERER, "referrer-from-header"))
      val result             = controller.indexDeprecated(Some("my-test-service"), None)(requestWithHeaders)

      status(result) should be(SEE_OTHER)
      val queryString = s"service=my-test-service&referrerUrl=referrer-from-header"
      redirectLocation(result) should be(Some(s"/contact/report-technical-problem?$queryString"))
    }
  }

  "Reporting a problem via the standalone page" should {
    "redirect to a Thank You html page for a valid request" in {
      setHmrcConnectorResponse(Future.successful(TicketId(123)))

      val request = generateRequest(isAjaxRequest = false)
      val result  = controller.submit(None, None)(request)

      status(result)             should be(SEE_OTHER)
      redirectLocation(result) shouldBe Some("/contact/report-technical-problem/thanks")
    }

    "bind the referrerUrl parameter if provided in the URL" in {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          meq("John Densmore"),
          meq("name@mail.com"),
          meq(controller.problemMessage("Some Action", "Some Error")),
          meq("referrer-from-url"),
          meq(true),
          any[Request[AnyRef]](),
          meq(None),
          meq(None),
          meq(None),
          any[TicketConstants]
        )(using any[HeaderCarrier])
      ).thenReturn(Future.successful(TicketId(123)))

      val request           = generateRequest(isAjaxRequest = true)
      val requestWithHeader = request.withHeaders((REFERER, "referrer-from-request"))
      val result            = controller.submit(None, Some("referrer-from-url"))(requestWithHeader)

      status(result)             should be(SEE_OTHER)
      redirectLocation(result) shouldBe Some("/contact/report-technical-problem/thanks")
    }

    "return Bad Request and page with validation error for invalid input" in {
      val result = controller.submit(None, None)(generateInvalidRequest(isAjaxRequest = false))

      status(result) should be(BAD_REQUEST)
      verifyNoInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request, page with validation error, and correct submit URL if no service URL param for invalid input" in {
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

      val result = controller.submit(None, None)(request)

      status(result) should be(BAD_REQUEST)
      verifyNoInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
      document
        .body()
        .select("form[id=error-feedback-form]")
        .first
        .attr("action")                                       shouldBe s"/contact/report-technical-problem"
    }

    "return Bad Request and page with validation error if the name has invalid characters" in {
      val request = generateRequest(
        isAjaxRequest = false,
        name = """<a href="blah.com">something</a>"""
      )

      val submit = controller.submit(None, None)(request)
      val page   = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request and page with validation error if the email has invalid syntax (for Deskpro)" in {
      val request = generateRequest(isAjaxRequest = false, email = "a.a.a")
      val submit  = controller.submit(None, None)(request)
      val page    = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyNoInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Internal Server Error and error page if the Deskpro ticket creation fails" in {
      setHmrcConnectorResponse(Future.failed(new Exception("failed")))

      val request = generateRequest(isAjaxRequest = false)
      val result  = controller.submit(None, None)(request)
      status(result) should be(INTERNAL_SERVER_ERROR)

      val document = Jsoup.parse(contentAsString(result))
      document.text() should include("Try again later.")
    }
  }

  "Requesting the standalone thanks page" should {
    "return OK and valid html" in {
      val result = controller.thanks()(FakeRequest())

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be(0)
      document.getElementsByClass("govuk-panel__title").text()  should be(
        "We have received your technical problem report"
      )
    }
  }
}

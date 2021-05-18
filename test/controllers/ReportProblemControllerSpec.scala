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
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.{ExecutionContext, Future}

class ReportProblemControllerSpec extends AnyWordSpec with GuiceOneAppPerSuite with Matchers {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"                          -> false,
        "metrics.enabled"                      -> false,
        "enablePlayFrontendProblemReportsForm" -> true
      )
      .build()

  implicit val messages: Messages =
    app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  "Requesting the standalone page" should {
    "return OK and valid HTML" in new TestScope {
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
  }

  "Requesting the deprecated standalone page" should {
    "redirect to the non-deprecated page with the REFERER passed via the URL" in new TestScope {
      val requestWithHeaders = FakeRequest().withHeaders((REFERER, "url-to-persist"))
      val result = controller.indexDeprecated(Some("my-test-service"))(requestWithHeaders)

      status(result) should be(SEE_OTHER)
      val queryString = s"service=my-test-service&referrerUrl=url-to-persist"
      redirectLocation(result) should be(Some(s"/contact/report-technical-problem?$queryString"))
    }
  }

  "Requesting the partial" should {
    "return OK and valid HTML" in new TestScope {
      val result = controller.partialIndex(
        preferredCsrfToken = None,
        service = Some("my-test-service")
      )(FakeRequest())

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document
        .body()
        .select("form[id=error-feedback-form]")
        .first
        .attr("action") should endWith("/contact/problem_reports")

      document.getElementById("error-feedback-form")            should not be null
      document.getElementsByClass("govuk-error-summary").size() should be(0)
    }
  }

  "Requesting the ajax partial" should {
    "return OK and valid HTML" in new TestScope {
      val result = controller.partialAjaxIndex(
        service = Some("my-test-service")
      )(FakeRequest())

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document
        .body()
        .select("form[id=error-feedback-form]")
        .first
        .attr("action") should endWith("/contact/problem_reports")

      document.getElementById("error-feedback-form")            should not be null
      document.getElementsByClass("govuk-error-summary").size() should be(0)
    }
  }

  "Reporting a problem via the standalone page" should {
    "redirect to a Thank You html page for a valid request" in new TestScope {
      hrmcConnectorWillReturnTheTicketId

      val request = generateRequest(isAjaxRequest = false)
      val result  = controller.submit(None, None)(request)

      status(result)             should be(SEE_OTHER)
      redirectLocation(result) shouldBe Some("/contact/report-technical-problem/thanks")
    }

    "return Bad Request and page with validation error for invalid input" in new TestScope {
      val result = controller.submit(None, None)(generateInvalidRequest(isAjaxRequest = false))

      status(result) should be(BAD_REQUEST)
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request, page with validation error, and correct submit URL if no service URL param for invalid input" in new TestScope {
      val headers = Seq((REFERER, deskproReferrer), ("User-Agent", "iAmAUserAgent"))
      val request = FakeRequest()
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
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
      document
        .body()
        .select("form[id=error-feedback-form]")
        .first
        .attr("action")                                       shouldBe s"/contact/report-technical-problem"
    }

    "return Bad Request and page with validation error if the name has invalid characters" in new TestScope {
      val request = generateRequest(
        isAjaxRequest = false,
        name = """<a href="blah.com">something</a>"""
      )

      val submit = controller.submit(None, None)(request)
      val page   = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Bad Request and page with validation error if the email has invalid syntax (for Deskpro)" in new TestScope {
      val request = generateRequest(isAjaxRequest = false, email = "a@a")
      val submit  = controller.submit(None, None)(request)
      val page    = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return Internal Server Error and error page if the Deskpro ticket creation fails" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          meq("John Densmore"),
          meq("name@mail.com"),
          meq("Support Request"),
          meq(controller.problemMessage("Some Action", "Some Error")),
          meq("/contact/problem_reports"),
          meq(false),
          any[Request[AnyRef]](),
          meq(None),
          meq(None),
          meq(None)
        )(any(classOf[HeaderCarrier]))
      ).thenReturn(Future.failed(new Exception("failed")))

      val request = generateRequest(isAjaxRequest = false)
      val result  = controller.submit(None, None)(request)
      status(result) should be(INTERNAL_SERVER_ERROR)

      val document = Jsoup.parse(contentAsString(result))
      document.text() should include("Try again later.")
    }
  }

  "Reporting a problem via the partial via Ajax" should {
    "return 200 and a valid json for a valid request" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          meq("John Densmore"),
          meq("name@mail.com"),
          meq("Support Request"),
          meq(controller.problemMessage("Some Action", "Some Error")),
          meq("/contact/problem_reports"),
          meq(true),
          any[Request[AnyRef]](),
          meq(None),
          meq(None),
          meq(None)
        )(any(classOf[HeaderCarrier]))
      ).thenReturn(Future.successful(TicketId(123)))

      val request = generateRequest(isAjaxRequest = true)
      val result  = controller.submitDeprecated(None)(request)

      status(result) should be(200)

      val message = contentAsJson(result).\("message").as[String]
      contentAsJson(result).\("status").as[String] shouldBe "OK"

      message should include("<h2 id=\"feedback-thank-you-header\">Thank you</h2>")
      message should include("Someone will get back to you within 2 working days.")
    }

    "return Bad Request and JSON with error status for invalid input" in new TestScope {
      val result = controller.submitDeprecated(None)(generateInvalidRequest(isAjaxRequest = true))
      status(result) should be(BAD_REQUEST)

      verifyZeroInteractions(hmrcDeskproConnector)
      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }

    "return Bad Request and JSON with error status if the email has invalid syntax (for DeskPRO)" in new TestScope {
      val request = generateRequest(isAjaxRequest = true, email = "a@a")
      val submit  = controller.submitDeprecated(None)(request)

      status(submit) should be(BAD_REQUEST)

      verifyZeroInteractions(hmrcDeskproConnector)
      contentAsJson(submit).\("status").as[String] shouldBe "ERROR"
    }

    "return Bad Request and JSON with error status if the name has invalid characters" in new TestScope {
      val request = generateRequest(
        isAjaxRequest = true,
        name = """<a href="blah.com">something</a>"""
      )

      val submit = controller.submitDeprecated(None)(request)
      status(submit) should be(BAD_REQUEST)

      verifyZeroInteractions(hmrcDeskproConnector)
      contentAsJson(submit).\("status").as[String] shouldBe "ERROR"
    }

    "return Bad Request and JSON with error status if the Deskpro ticket creation fails" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          meq("John Densmore"),
          meq("name@mail.com"),
          meq("Support Request"),
          meq(controller.problemMessage("Some Action", "Some Error")),
          meq("/contact/problem_reports"),
          meq(false),
          any[Request[AnyRef]](),
          meq(None),
          meq(None),
          meq(None)
        )(any(classOf[HeaderCarrier]))
      ).thenReturn(Future.failed(new Exception("failed")))

      val request = generateRequest(isAjaxRequest = true)
      val result  = controller.submitDeprecated(None)(request)
      status(result)                                 should be(INTERNAL_SERVER_ERROR)
      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }
  }

  "Reporting a problem via the deprecated non-Ajax partial" should {
    "redirect to a Thank You html page for a valid request" in new TestScope {
      hrmcConnectorWillReturnTheTicketId

      val request = generateRequest(isAjaxRequest = false)
      val result  = controller.submitDeprecated(None)(request)

      status(result)             should be(SEE_OTHER)
      redirectLocation(result) shouldBe Some("/contact/report-technical-problem/thanks")
    }

    "return 400 and JSON containing validation errors for invalid input" in new TestScope {
      val result = controller.submitDeprecated(None)(generateInvalidRequest(isAjaxRequest = false))

      status(result) should be(BAD_REQUEST)
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "fail if the email has invalid syntax (for DeskPRO)" in new TestScope {
      val request = generateRequest(isAjaxRequest = false, email = "a@a")
      val submit  = controller.submitDeprecated(None)(request)
      val page    = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "fail if the name has invalid characters" in new TestScope {
      val request = generateRequest(
        isAjaxRequest = false,
        name = """<a href="blah.com">something</a>"""
      )

      val submit = controller.submitDeprecated(None)(request)
      val page   = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe BAD_REQUEST
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementsByClass("govuk-error-summary").size() should be > 0
    }

    "return error page if the Deskpro ticket creation fails with Javascript disabled" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          meq("John Densmore"),
          meq("name@mail.com"),
          meq("Support Request"),
          meq(controller.problemMessage("Some Action", "Some Error")),
          meq("/contact/problem_reports"),
          meq(false),
          any[Request[AnyRef]](),
          meq(None),
          meq(None),
          meq(None)
        )(any(classOf[HeaderCarrier]))
      ).thenReturn(Future.failed(new Exception("failed")))

      val request = generateRequest(isAjaxRequest = false)
      val result  = controller.submitDeprecated(None)(request)
      status(result) should be(500)

      val document = Jsoup.parse(contentAsString(result))
      document.text() should include("Try again later.")
    }

  }

  "Requesting the standalone thanks page" should {
    "return OK and valid html" in new TestScope {
      val result = controller.thanks()(FakeRequest())

      status(result) should be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").size() should be(0)
      document.getElementsByClass("govuk-body").text()          should be(
        "Someone will get back to you within 2 working days."
      )
    }
  }

  class TestScope extends MockitoSugar {

    val authConnector = new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[A] =
        Future.successful(
          Json.parse("{ \"allEnrolments\" : []}").as[A](retrieval.reads)
        )
    }

    val reportProblemPage      = app.injector.instanceOf[views.html.ReportProblemPage]
    val confirmationPage       = app.injector.instanceOf[views.html.ReportProblemConfirmationPage]
    val errorPage              = app.injector.instanceOf[views.html.InternalErrorPage]
    val errorFeedbackForm      = app.injector.instanceOf[views.html.partials.error_feedback]
    val errorFeedbackFormInner = app.injector.instanceOf[views.html.partials.error_feedback_inner]
    val ticketCreatedBody      = app.injector.instanceOf[views.html.partials.ticket_created_body]

    val controller = new ReportProblemController(
      mock[HmrcDeskproConnector],
      authConnector,
      Stubs.stubMessagesControllerComponents(messagesApi = app.injector.instanceOf[MessagesApi]),
      reportProblemPage,
      confirmationPage,
      errorFeedbackForm,
      errorFeedbackFormInner,
      ticketCreatedBody,
      errorPage
    )(new CFConfig(app.configuration), ExecutionContext.Implicits.global)

    val deskproName: String           = "John Densmore"
    val deskproEmail: String          = "name@mail.com"
    val deskproSubject: String        = "Support Request"
    val deskproProblemMessage: String =
      controller.problemMessage("Some Action", "Some Error")(
        app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))
      )
    val deskproReferrer: String       = "/contact/problem_reports"

    val hmrcDeskproConnector = controller.hmrcDeskproConnector

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

      FakeRequest()
        .withHeaders(headers: _*)
        .withFormUrlEncodedBody(
          "report-name"   -> name,
          "report-email"  -> email,
          "report-action" -> "Some Action",
          "report-error"  -> "Some Error",
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

      FakeRequest()
        .withHeaders(headers: _*)
        .withFormUrlEncodedBody("isJavascript" -> isAjaxRequest.toString)
    }

    def hmrcConnectorWillFail =
      mockHmrcConnector(Future.failed(new Exception("failed")))

    def hrmcConnectorWillReturnTheTicketId =
      mockHmrcConnector(Future.successful(TicketId(123)))

    private def mockHmrcConnector(result: Future[TicketId]) =
      when(
        hmrcDeskproConnector.createDeskProTicket(
          meq(deskproName),
          meq(deskproEmail),
          meq(deskproSubject),
          meq(deskproProblemMessage),
          meq(deskproReferrer),
          meq(false),
          any[Request[AnyRef]](),
          meq(None),
          meq(None),
          meq(None)
        )(any(classOf[HeaderCarrier]))
      ).thenReturn(result)
  }

}

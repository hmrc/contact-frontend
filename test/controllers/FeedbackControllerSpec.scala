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

import java.net.URLEncoder
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.{TicketConstants, TicketId}
import connectors.enrolments.EnrolmentsConnector
import helpers.BaseControllerSpec
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.*
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.{BackUrlValidator, RefererHeaderRetriever}

import scala.concurrent.Future

class FeedbackControllerSpec extends BaseControllerSpec {

  val ticketQueueConnector                        = mock[DeskproTicketQueueConnector]
  def ticketQueueConnectorWillFail()              = mockQueueConnector(Future.failed(new Exception("failed")))
  def ticketQueueConnectorWillReturnTheTicketId() = mockQueueConnector(Future.successful(TicketId(123)))

  val enrolmentsConnector: EnrolmentsConnector = mock[EnrolmentsConnector]
  when(enrolmentsConnector.maybeAuthenticatedUserEnrolments()(using any())(using any()))
    .thenReturn(Future.successful(None))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(ticketQueueConnector)
  }

  val feedbackName: String     = "John Densmore"
  val feedbackRating: String   = "2"
  val feedbackEmail: String    = "name@mail.com"
  val feedbackComment: String  = "Comments"
  val feedbackReferrer: String = "/contact/problem_reports"

  private def mockQueueConnector(result: Future[TicketId]) =
    when(
      ticketQueueConnector.createFeedback(
        name = any[String],
        email = any[String],
        rating = any[String],
        message = any[String],
        referrer = any[String],
        isJavascript = any[Boolean],
        any[Request[AnyRef]](),
        any[Option[Enrolments]],
        any[Option[String]],
        any[TicketConstants]
      )(using any[HeaderCarrier])
    ).thenReturn(result)

  def verifyRequestMade(comment: String = feedbackComment): Unit =
    verify(ticketQueueConnector).createFeedback(
      meq(feedbackName),
      meq(feedbackEmail),
      meq(feedbackRating),
      meq(comment),
      meq(feedbackReferrer),
      meq(true),
      any[Request[AnyRef]](),
      any[Option[Enrolments]],
      any[Option[String]],
      any[TicketConstants]
    )(using any[HeaderCarrier])

  val backUrlValidator = new BackUrlValidator() {
    override def validate(backUrl: String) = backUrl == "http://www.valid.url"
  }

  val playFrontendFeedbackPage             = instanceOf[views.html.FeedbackPage]
  val playFrontendFeedbackConfirmationPage = instanceOf[views.html.FeedbackConfirmationPage]
  val errorPage                            = instanceOf[views.html.InternalErrorPage]

  val controller = new FeedbackController(
    ticketQueueConnector,
    enrolmentsConnector,
    backUrlValidator,
    Stubs.stubMessagesControllerComponents(),
    playFrontendFeedbackConfirmationPage,
    playFrontendFeedbackPage,
    errorPage,
    new RefererHeaderRetriever
  )

  def generateRequest(
    javascriptEnabled: Boolean = true,
    name: String = feedbackName,
    email: String = feedbackEmail,
    comments: String = feedbackComment,
    backUrl: Option[String] = None,
    canOmitComments: Boolean = false
  ) = {

    val fields = Map(
      "feedback-name"     -> name,
      "feedback-email"    -> email,
      "feedback-rating"   -> "2",
      "feedback-comments" -> comments,
      "csrfToken"         -> "token",
      "referrer"          -> feedbackReferrer,
      "canOmitComments"   -> canOmitComments.toString,
      "isJavascript"      -> javascriptEnabled.toString
    ) ++ backUrl.map("backUrl" -> _)

    FakeRequest("POST", "/")
      .withHeaders((REFERER, feedbackReferrer), ("User-Agent", "iAmAUserAgent"))
      .withFormUrlEncodedBody(fields.toSeq: _*)
  }

  def generateInvalidRequest() = FakeRequest("POST", "/")
    .withHeaders((REFERER, feedbackReferrer), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("isJavascript" -> "true")

  def generateInvalidRequestWithBackUrlAndService() = FakeRequest("POST", "/")
    .withHeaders((REFERER, feedbackReferrer), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody(
      "isJavascript"    -> "true",
      "backUrl"         -> "http://www.back.url",
      "service"         -> "someService",
      "canOmitComments" -> "true",
      "referrer"        -> "http://www.referrer.url"
    )

  val request = generateRequest()

  val requestWithBackLink = generateRequest(backUrl = Some("http://www.back.url"))

  "feedbackForm" should {
    "include 'service', 'backUrl' and 'canOmitComments' hidden fields" in {
      val result = controller.index(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true,
        referrerUrl = None
      )(FakeRequest("GET", "/foo"))

      val page = Jsoup.parse(contentAsString(result))
      page.body().select("input[name=service]").first.attr("value")   shouldBe "any-service"
      page.body().select("input[name=backUrl]").first.attr("value")   shouldBe "/any-service"
      page.body().select("input[name=canOmitComments]").attr("value") shouldBe "true"
    }
  }

  "index" should {
    "include 'service', 'backUrl' and 'canOmitComments' hidden fields and bind to submit URL" in {
      val result = controller.index(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true,
        referrerUrl = None
      )(FakeRequest("GET", "/foo"))

      val page = Jsoup.parse(contentAsString(result))

      val encodedBackUrl = URLEncoder.encode("/any-service", "UTF-8")
      val queryString    = s"service=any-service&backUrl=$encodedBackUrl&canOmitComments=true"
      page
        .body()
        .select("form[id=feedback-form]")
        .first
        .attr("action") shouldBe s"/contact/beta-feedback?$queryString"

      page.body().select("input[name=service]").first.attr("value")   shouldBe "any-service"
      page.body().select("input[name=backUrl]").first.attr("value")   shouldBe "/any-service"
      page.body().select("input[name=canOmitComments]").attr("value") shouldBe "true"
    }

    "include 'referrer' hidden field from query string when passed as both parameter and headers" in {
      val result = controller.index(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true,
        referrerUrl = Some("any-referring-parameter")
      )(FakeRequest("GET", "/foo").withHeaders((REFERER, "any-referring-header")))

      val page = Jsoup.parse(contentAsString(result))

      page.body().select("input[name=referrer]").first.attr("value") shouldBe "any-referring-parameter"
    }

    "include 'referrer' hidden field from header when passed in request headers only" in {
      val result = controller.index(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true,
        referrerUrl = None
      )(FakeRequest("GET", "/foo").withHeaders((REFERER, "any-referring-header")))

      val page = Jsoup.parse(contentAsString(result))

      page.body().select("input[name=referrer]").first.attr("value") shouldBe "any-referring-header"
    }

    "set 'referrer' hidden field to n/a if not passed in query string or request headers" in {
      val result = controller.index(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true,
        referrerUrl = None
      )(FakeRequest("GET", "/foo"))

      val page = Jsoup.parse(contentAsString(result))

      page.body().select("input[name=referrer]").first.attr("value") shouldBe "n/a"
    }

  }

  "Submitting the feedback" should {

    "redirect to confirmation page without 'back' button if 'back' link not provided" in {

      ticketQueueConnectorWillReturnTheTicketId()

      val result = controller.submit()(request)

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks")

      verifyRequestMade()
    }

    "show errors if some form not filled in correctly" in {

      ticketQueueConnectorWillReturnTheTicketId()

      val result = controller.submit()(generateInvalidRequest())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("govuk-error-message") shouldNot be(empty)

      verifyNoInteractions(ticketQueueConnector)
    }

    "succeed with comment if 'canOmitComments' flag is true" in {
      ticketQueueConnectorWillReturnTheTicketId()

      val result =
        controller.submit()(generateRequest(comments = "Some comment", canOmitComments = true))

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks")

      verifyRequestMade(comment = "Some comment")
    }

    "succeed without comment if 'canOmitComments' flag is true" in {
      ticketQueueConnectorWillReturnTheTicketId()

      val result = controller.submit()(generateRequest(comments = "", canOmitComments = true))

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks")

      verifyRequestMade(comment = "No comment given")
    }

    "fail without comment if 'canOmitComments' flag is false" in {
      ticketQueueConnectorWillReturnTheTicketId()

      val result = controller.submit()(generateRequest(comments = "", canOmitComments = false))

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("govuk-error-message") shouldNot be(empty)

      verifyNoInteractions(ticketQueueConnector)
    }

    "include 'service', 'backUrl', 'canOmitComments' and 'referrer' fields in the returned page if form not filled in correctly" in {

      ticketQueueConnectorWillReturnTheTicketId()

      val result = controller.submit()(generateInvalidRequestWithBackUrlAndService())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().select(".govuk-error-message").size should be > 0

      val backUrl            = "http://www.back.url"
      val referrerUrl        = "http://www.referrer.url"
      val encodedBackUrl     = URLEncoder.encode(backUrl, "UTF-8")
      val encodedReferrerUrl = URLEncoder.encode(referrerUrl, "UTF-8")

      val queryString =
        s"service=someService&backUrl=$encodedBackUrl&canOmitComments=true&referrerUrl=$encodedReferrerUrl"

      page.body().select("form[id=feedback-form]").first.attr("action") shouldBe s"/contact/beta-feedback?$queryString"
      page.body().select("input[name=service]").first.attr("value")     shouldBe "someService"
      page.body().select("input[name=canOmitComments]").attr("value")   shouldBe "true"
      page.body().select("input[name=backUrl]").first.attr("value")     shouldBe backUrl
      page.body().select("input[name=referrer]").attr("value")          shouldBe referrerUrl
    }

    "return service error page if call to backend service failed" in {

      ticketQueueConnectorWillFail()

      val result = controller.submit()(request)
      status(result) shouldBe 500
      val page = Jsoup.parse(contentAsString(result))
      page.body().select("h1").first.text() shouldBe "deskpro.error.page.heading"
    }

    "redirect to confirmation page with 'back' button if 'back' link provided" in {

      ticketQueueConnectorWillReturnTheTicketId()

      val result = controller.submit()(requestWithBackLink)

      status(result) should be(303)
      val encodedBackUrl: String = URLEncoder.encode("http://www.back.url", "UTF-8")
      redirectLocation(result) shouldBe Some(
        s"/contact/beta-feedback/thanks?backUrl=$encodedBackUrl"
      )

      verifyRequestMade()
    }
  }

  "Feedback confirmation page " should {
    "not contain back button if not requested" in {

      val submit = controller.thanks()(request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID"))
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-link") should have size 1
    }

    "contain back button if requested and the back url is valid" in {

      val submit = controller.thanks(backUrl = Some("http://www.valid.url"))(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-back-link") should have size 1

      page.body().select(".govuk-back-link").get(0).attr("href") shouldBe "http://www.valid.url"
    }

    "not contain back link if requested and the back url is invalid" in {

      val submit = controller.thanks(backUrl = Some("http://www.invalid.url"))(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-link") should have size 1

    }
  }
}

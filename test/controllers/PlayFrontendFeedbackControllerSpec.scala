/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import java.net.URLEncoder

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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.BackUrlValidator

import scala.concurrent.{ExecutionContext, Future}

class PlayFrontendFeedbackControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "enablePlayFrontendFeedbackForm" -> true)
      .build()

  "feedbackForm" should {
    "include 'service', 'backUrl' and 'canOmitComments' hidden fields" in new TestScope {
      val result = controller.feedbackForm(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true
      )(FakeRequest("GET", "/foo"))

      val page = Jsoup.parse(contentAsString(result))
      page.body().select("input[name=service]").first.attr("value")   shouldBe "any-service"
      page.body().select("input[name=backUrl]").first.attr("value")   shouldBe "/any-service"
      page.body().select("input[name=canOmitComments]").attr("value") shouldBe "true"
    }
  }

  "unauthenticatedFeedbackForm" should {
    "include 'service', 'backUrl' and 'canOmitComments' hidden fields" in new TestScope {
      val result = controller.unauthenticatedFeedbackForm(
        service = Some("any-service"),
        backUrl = Some("/any-service"),
        canOmitComments = true
      )(FakeRequest("GET", "/foo"))

      val page = Jsoup.parse(contentAsString(result))
      page.body().select("input[name=service]").first.attr("value")   shouldBe "any-service"
      page.body().select("input[name=backUrl]").first.attr("value")   shouldBe "/any-service"
      page.body().select("input[name=canOmitComments]").attr("value") shouldBe "true"
    }
  }

  "feedbackPartialForm" should {
    val submitUrl       = "https:/abcdefg.com"
    val csrfToken       = "CSRF"
    val service         = Some("scp")
    val referer         = Some("https://www.example.com/some-service")
    val canOmitComments = false

    "use the (deprecated) referer parameter if supplied" in new TestScope {
      val result =
        controller.feedbackPartialForm(submitUrl, csrfToken, service, referer, canOmitComments, None)(request)

      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementById("referrer").attr("value") shouldBe "https://www.example.com/some-service"
    }

    "use the referrerUrl parameter if supplied" in new TestScope {
      val referrerUrl = Some("https://www.other-example.com/some-service")

      val result =
        controller.feedbackPartialForm(submitUrl, csrfToken, service, referer, canOmitComments, referrerUrl)(request)

      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementById("referrer").attr("value") shouldBe "https://www.other-example.com/some-service"
    }

  }

  "Submitting the feedback for unauthenticated user" should {

    "redirect to confirmation page without 'back' button if 'back' link not provided" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(request)

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks-unauthenticated")

      verifyRequestMade()
    }

    "show errors if some form not filled in correctly" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(generateInvalidRequest())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("govuk-error-message") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }

    "succeed with comment if 'canOmitComments' flag is true" in new TestScope {
      hmrcConnectorWillReturnTheTicketId()

      val result =
        controller.submitUnauthenticated()(generateRequest(comments = "Some comment", canOmitComments = true))

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks-unauthenticated")

      verifyRequestMade(comment = "Some comment")
    }

    "succeed without comment if 'canOmitComments' flag is true" in new TestScope {
      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(generateRequest(comments = "", canOmitComments = true))

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks-unauthenticated")

      verifyRequestMade(comment = "No comment given")
    }

    "fail without comment if 'canOmitComments' flag is false" in new TestScope {
      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(generateRequest(comments = "", canOmitComments = false))

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("govuk-error-message") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }

    "include 'service', 'backUrl' and 'canOmitComments' fields in the returned page if form not filled in correctly" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(generateInvalidRequestWithBackUrlAndService())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().select(".govuk-error-message").size should be > 0

      page.body().select("input[name=service]").first.attr("value")   shouldBe "someService"
      page.body().select("input[name=backUrl]").first.attr("value")   shouldBe "http://www.back.url"
      page.body().select("input[name=canOmitComments]").attr("value") shouldBe "true"

    }

    "show errors if call to hmrc-deskpro failed" in new TestScope {

      hmrcConnectorWillFail()

      an[Exception] shouldBe thrownBy(await(controller.submitUnauthenticated()(request)))

    }

    "redirect to confirmation page with 'back' button if 'back' link provided" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(requestWithBackLink)

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some(
        s"/contact/beta-feedback/thanks-unauthenticated?backUrl=${URLEncoder.encode("http://www.back.url", "UTF-8")}"
      )

      verifyRequestMade()
    }
  }

  "Submitting feedback for authenticated user" should {
    "redirect to confirmation page without 'back' button if 'back' link not provided" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submit()(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks")

      verifyRequestMade()
    }

    "redirect to confirmation page with 'back' button if 'back' link provided" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submit()(requestWithBackLink.withSession(SessionKeys.authToken -> "authToken"))

      status(result)             should be(303)
      redirectLocation(result) shouldBe Some(
        s"/contact/beta-feedback/thanks?backUrl=${URLEncoder.encode("http://www.back.url", "UTF-8")}"
      )

      verifyRequestMade()
    }
  }

  "Feedback confirmation page for authenticated user " should {
    "not contain back button if not requested" in new TestScope {

      val submit = controller.thanks()(request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID"))
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-link") should have size 1
    }

    "contain back button if requested and the back url is valid" in new TestScope {

      val submit = controller.thanks(backUrl = Some("http://www.valid.url"))(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-link") should have size 2

      page.body().select(".govuk-link").get(1).attr("href") shouldBe "http://www.valid.url"
    }

    "not contain back link if requested and the back url is invalid" in new TestScope {

      val submit = controller.thanks(backUrl = Some("http://www.invalid.url"))(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-link") should have size 1

    }
  }

  "Feedback confirmation page for anonymous user " should {
    "not contain back button if not requested" in new TestScope {

      val submit = controller.unauthenticatedThanks()(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-back-link").first shouldBe null

    }

    "contain back button if requested and the back url is valid" in new TestScope {

      val submit = controller.unauthenticatedThanks(backUrl = Some("http://www.valid.url"))(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-link")                       should have size 2
      page.body().select(".govuk-link").get(1).attr("href") shouldBe "http://www.valid.url"
    }

    "not contain back button if requested and the back url is invalid" in new TestScope {

      val submit = controller.unauthenticatedThanks(backUrl = Some("http://www.invalid.url"))(
        request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID")
      )
      val page   = Jsoup.parse(contentAsString(submit))

      page.body().select(".govuk-back-link").first shouldBe null

    }
  }

  "Submitting the partial feedback form" should {

    "show errors if the form is not filled in correctly" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitFeedbackPartialForm("tstUrl")(generateInvalidRequest())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-message") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }

    "allow comments to be empty if the canOmitComments flag is set to true" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result =
        controller.submitFeedbackPartialForm("tstUrl")(generateRequest(comments = "", canOmitComments = true))

      status(result) should be(200)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-message") should be(empty)

      verifyRequestMade("No comment given")
    }

    "show errors if no comments are provided and the canOmitComments flag is set to false" in new TestScope {

      hmrcConnectorWillReturnTheTicketId()

      val result =
        controller.submitFeedbackPartialForm("tstUrl")(generateRequest(comments = "", canOmitComments = false))

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-message") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }
  }

  class TestScope extends MockitoSugar {

    val hmrcDeskproConnector = mock[HmrcDeskproConnector]

    def hmrcConnectorWillFail() = mockHmrcConnector(Future.failed(new Exception("failed")))

    def hmrcConnectorWillReturnTheTicketId() = mockHmrcConnector(Future.successful(TicketId(123)))

    val feedbackName: String     = "John Densmore"
    val feedbackRating: String   = "2"
    val feedbackEmail: String    = "name@mail.com"
    val feedbackComment: String  = "Comments"
    val feedbackReferrer: String = "/contact/problem_reports"

    private def mockHmrcConnector(result: Future[TicketId]) =
      when(
        hmrcDeskproConnector.createFeedback(
          name = any[String],
          email = any[String],
          rating = any[String],
          subject = any[String],
          message = any[String],
          referrer = any[String],
          isJavascript = any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
      ).thenReturn(result)

    def verifyRequestMade(comment: String = feedbackComment): Unit =
      verify(hmrcDeskproConnector).createFeedback(
        meq(feedbackName),
        meq(feedbackEmail),
        meq(feedbackRating),
        meq("Beta feedback submission"),
        meq(comment),
        meq(feedbackReferrer),
        meq(true),
        any[Request[AnyRef]](),
        any[Option[Enrolments]],
        any[Option[String]],
        any[Option[String]]
      )(any[HeaderCarrier])

    val authConnector = new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[A] =
        Future.successful(Json.parse("{ \"allEnrolments\" : []}").as[A](retrieval.reads))
    }

    val backUrlValidator = new BackUrlValidator() {
      override def validate(backUrl: String) = backUrl == "http://www.valid.url"
    }

    val feedbackPage                         = app.injector.instanceOf[views.html.feedback]
    val feedbackConfirmationPage             = app.injector.instanceOf[views.html.feedback_confirmation]
    val feedbackPartialForm                  = app.injector.instanceOf[views.html.partials.feedback_form]
    val feedbackFormConfirmation             = app.injector.instanceOf[views.html.partials.feedback_form_confirmation]
    val playFrontendFeedbackPage             = app.injector.instanceOf[views.html.FeedbackPage]
    val playFrontendFeedbackConfirmationPage =
      app.injector.instanceOf[views.html.FeedbackConfirmationPage]

    val controller = new FeedbackController(
      hmrcDeskproConnector,
      authConnector,
      backUrlValidator,
      app.configuration,
      Stubs.stubMessagesControllerComponents(),
      feedbackPage,
      feedbackConfirmationPage,
      playFrontendFeedbackConfirmationPage,
      feedbackPartialForm,
      feedbackFormConfirmation,
      playFrontendFeedbackPage
    )(new CFConfig(app.configuration), ExecutionContext.Implicits.global)

    val enrolments = Some(Enrolments(Set()))

    def generateRequest(
      javascriptEnabled: Boolean = true,
      name: String = feedbackName,
      email: String = feedbackEmail,
      comments: String = feedbackComment,
      backUrl: Option[String] = None,
      canOmitComments: Boolean = false
    ) = {

      val fields = Map(
        "feedback-name"          -> name,
        "feedback-email"         -> email,
        "feedback-rating"        -> "2",
        "feedback-comments"      -> comments,
        "csrfToken"              -> "token",
        "referrer"               -> feedbackReferrer,
        "canOmitComments"        -> canOmitComments.toString,
        "isJavascript"           -> javascriptEnabled.toString
      ) ++ backUrl.map("backUrl" -> _)

      FakeRequest()
        .withHeaders((REFERER, feedbackReferrer), ("User-Agent", "iAmAUserAgent"))
        .withFormUrlEncodedBody(fields.toSeq: _*)
    }

    def generateInvalidRequest()                      = FakeRequest()
      .withHeaders((REFERER, feedbackReferrer), ("User-Agent", "iAmAUserAgent"))
      .withFormUrlEncodedBody("isJavascript" -> "true")

    def generateInvalidRequestWithBackUrlAndService() = FakeRequest()
      .withHeaders((REFERER, feedbackReferrer), ("User-Agent", "iAmAUserAgent"))
      .withFormUrlEncodedBody(
        "isJavascript"    -> "true",
        "backUrl"         -> "http://www.back.url",
        "service"         -> "someService",
        "canOmitComments" -> "true"
      )

    val request = generateRequest()

    val requestWithBackLink = generateRequest(backUrl = Some("http://www.back.url"))
  }

}

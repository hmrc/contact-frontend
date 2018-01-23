package controllers

import java.net.URLEncoder

import config.CFConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Environment}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import util.BackUrlValidator

import scala.concurrent.{ExecutionContext, Future}

class FeedbackControllerSpec extends UnitSpec with WithFakeApplication {

  "Submitting the feedback for unauthenticated user" should {

    "redirect to confirmation page without 'back' button if 'back' link not provided" in new FeedbackControllerApplication(fakeApplication) {

      hrmcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(request)

      status(result) should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks-unauthenticated")

      verifyRequestMade
    }

    "show errors if some form not filled in correctly" in new FeedbackControllerApplication(fakeApplication) {

      hrmcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(generateInvalidRequest())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-notification") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }

    "include 'server' and 'backUrl' fields in the returned page if form not filled in correctly" in new FeedbackControllerApplication(fakeApplication) {

      hrmcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(generateInvalidRequestWithBackUrlAndService())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-notification") shouldNot be(empty)
      page.body().getElementById("feedbackService").attr("value") shouldBe "someService"
      page.body().getElementById("feedbackBackUrl").attr("value") shouldBe "http://www.back.url"


    }

    "show errors if call to hmrc-deskpro failed" in new FeedbackControllerApplication(fakeApplication) {

      hmrcConnectorWillFail()

      an[Exception] shouldBe thrownBy (await(controller.submitUnauthenticated()(request)))

    }

    "redirect to confirmation page with 'back' button if 'back' link provided" in new FeedbackControllerApplication(fakeApplication) {

      hrmcConnectorWillReturnTheTicketId()

      val result = controller.submitUnauthenticated()(requestWithBackLink)

      status(result) should be(303)
      redirectLocation(result) shouldBe Some(s"/contact/beta-feedback/thanks-unauthenticated?backUrl=${URLEncoder.encode("http://www.back.url", "UTF-8")}")

      verifyRequestMade
    }
  }

  "Submitting feedback for authenticated user" should {
    "redirect to confirmation page without 'back' button if 'back' link not provided" in new FeedbackControllerApplication(fakeApplication) {

      hrmcConnectorWillReturnTheTicketId()

      val result = controller.submit()(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(303)
      redirectLocation(result) shouldBe Some("/contact/beta-feedback/thanks")

      verifyRequestMade
    }

    "redirect to confirmation page with 'back' button if 'back' link provided" in new FeedbackControllerApplication(fakeApplication) {

      hrmcConnectorWillReturnTheTicketId()

      val result = controller.submit()(requestWithBackLink.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(303)
      redirectLocation(result) shouldBe Some(s"/contact/beta-feedback/thanks?backUrl=${URLEncoder.encode("http://www.back.url", "UTF-8")}")

      verifyRequestMade
    }
  }

  "Feedback confirmation page" should {
    "not contain back button if not requested" in new FeedbackControllerApplication(fakeApplication) {

      val submit = controller.thanks()(request.withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID"))
      val page = Jsoup.parse(contentAsString(submit))

      page.body().getElementById("feedback-back") shouldBe null

    }

    "contain back button if requested and the back url is valid" in new FeedbackControllerApplication(fakeApplication) {

      val submit = controller.thanks(backUrl = Some("http://www.valid.url"))(request.
        withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID"))
      val page = Jsoup.parse(contentAsString(submit))

      page.body().getElementById("feedback-back").attr("href") shouldBe "http://www.valid.url"

    }

    "not contain back button if requested and the back url is invalid" in new FeedbackControllerApplication(fakeApplication) {

      val submit = controller.thanks(backUrl = Some("http://www.invalid.url"))(request.
        withSession(SessionKeys.authToken -> "authToken", "ticketId" -> "TID"))
      val page = Jsoup.parse(contentAsString(submit))

      page.body().getElementById("feedback-back") shouldBe null

    }

}

class FeedbackControllerApplication(app: Application) extends MockitoSugar {

  val hmrcDeskproConnector = mock[HmrcDeskproConnector]

  def hmrcConnectorWillFail() = mockHmrcConnector(Future.failed(new Exception("failed")))

  def hrmcConnectorWillReturnTheTicketId() = mockHmrcConnector(Future.successful(TicketId(123)))

  val feedbackName: String = "John Densmore"
  val feedbackRating: String = "2"
  val feedbackEmail: String = "name@mail.com"
  val feedbackComment: String = "Comments"
  val feedbackReferer: String = "/contact/problem_reports"

  private def mockHmrcConnector(result: Future[TicketId]) = {
    when(hmrcDeskproConnector.createFeedback(
      name = any[String],
      email = any[String],
      rating = any[String],
      subject = any[String],
      message = any[String],
      referrer = any[String],
      isJavascript = any[Boolean],
      any[Request[AnyRef]](),
      any[Option[Enrolments]],
      any[Option[String]])(any[HeaderCarrier])).thenReturn(result)
  }

  def verifyRequestMade: Unit = {
    verify(hmrcDeskproConnector).createFeedback(
      meq(feedbackName),
      meq(feedbackEmail),
      meq(feedbackRating),
      meq("Beta feedback submission"),
      meq(feedbackComment),
      meq(feedbackReferer),
      meq(true),
      any[Request[AnyRef]](),
      any[Option[Enrolments]],
      any[Option[String]])(any[HeaderCarrier])
  }

  val authConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(Json.parse("{ \"allEnrolments\" : []}").as[A](retrieval.reads))
    }
  }

  val backUrlValidator = new BackUrlValidator() {
    override def validate(backUrl: String) = backUrl == "http://www.valid.url"
  }

  val environment: Environment = Environment.simple()

  val controller = new FeedbackController(hmrcDeskproConnector, authConnector, backUrlValidator, app.configuration, environment)(
    new CFConfig(environment, app.configuration), app.injector.instanceOf[MessagesApi])

  val enrolments = Some(Enrolments(Set()))

  def generateRequest(javascriptEnabled: Boolean = true, name: String = feedbackName, email: String = feedbackEmail, comments: String = feedbackComment, backUrl: Option[String] = None) = {

    val fields = Map("feedback-name" -> name,
      "feedback-email" -> email,
      "feedback-rating" -> "2",
      "feedback-comments" -> comments,
      "csrfToken" -> "token",
      "referer" -> feedbackReferer,
      "isJavascript" -> javascriptEnabled.toString) ++ backUrl.map("backUrl" -> _)

    FakeRequest()
      .withHeaders(("referer", feedbackReferer), ("User-Agent", "iAmAUserAgent"))
      .withFormUrlEncodedBody(fields.toSeq: _*)
  }

    def generateInvalidRequest() = FakeRequest()
      .withHeaders(("referer", feedbackReferer), ("User-Agent", "iAmAUserAgent"))
      .withFormUrlEncodedBody("isJavascript" -> "true")

    def generateInvalidRequestWithBackUrlAndService() = FakeRequest()
      .withHeaders(("referer", feedbackReferer), ("User-Agent", "iAmAUserAgent"))
      .withFormUrlEncodedBody("isJavascript" -> "true", "backUrl" -> "http://www.back.url", "service" -> "someService")

    val request = generateRequest()

    val requestWithBackLink = generateRequest(backUrl = Some("http://www.back.url"))
  }

}
package unit.controllers

import config.CFConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import controllers.PartialsController
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import util.BackUrlValidator

import scala.concurrent.{ExecutionContext, Future}

class PartialsControllerSpec extends UnitSpec with WithFakeApplication {

  "Submitting the feedback form" should {

    "show errors if the form is not filled in correctly" in new PartialsControllerApplication(fakeApplication) {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitFeedbackForm("tstUrl")(generateInvalidRequest())

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-notification") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }

    "allow comments to be empty if the canOmitComments flag is set to true" in new PartialsControllerApplication(fakeApplication) {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitFeedbackForm("tstUrl")(generateRequest(comments = "", canOmitComments = true))

      status(result) should be(200)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-notification") should be(empty)

      verifyRequestMade("No comment given")
    }

    "show errors if no comments are provided and the canOmitComments flag is set to false" in new PartialsControllerApplication(fakeApplication) {

      hmrcConnectorWillReturnTheTicketId()

      val result = controller.submitFeedbackForm("tstUrl")(generateRequest(comments = "", canOmitComments = false))

      status(result) should be(400)
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-notification") shouldNot be(empty)

      verifyZeroInteractions(hmrcDeskproConnector)
    }
  }

}

class PartialsControllerApplication(app: Application) extends MockitoSugar {

  val hmrcDeskproConnector = mock[HmrcDeskproConnector]

  def hmrcConnectorWillFail() = mockHmrcConnector(Future.failed(new Exception("failed")))

  def hmrcConnectorWillReturnTheTicketId() = mockHmrcConnector(Future.successful(TicketId(123)))

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

  def verifyRequestMade(comment: String): Unit = {
    verify(hmrcDeskproConnector).createFeedback(
      meq(feedbackName),
      meq(feedbackEmail),
      meq(feedbackRating),
      meq("Beta feedback submission"),
      meq(comment),
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

  val controller = new PartialsController(hmrcDeskproConnector, authConnector)(
    new CFConfig(environment, app.configuration), app.injector.instanceOf[MessagesApi])

  val enrolments = Some(Enrolments(Set()))

  def generateRequest(javascriptEnabled: Boolean = true, name: String = feedbackName, email: String = feedbackEmail, comments: String = feedbackComment, backUrl: Option[String] = None,
                      canOmitComments : Boolean = false) = {

    val fields = Map("feedback-name" -> name,
      "feedback-email" -> email,
      "feedback-rating" -> "2",
      "feedback-comments" -> comments,
      "csrfToken" -> "token",
      "referer" -> feedbackReferer,
      "canOmitComments" -> canOmitComments.toString,
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
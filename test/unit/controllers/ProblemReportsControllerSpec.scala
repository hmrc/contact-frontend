package controllers

import config.CFConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.{Application, Environment, Play}
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import util.DeskproEmailValidator

import scala.concurrent.{ExecutionContext, Future}

class ProblemReportsControllerSpec extends UnitSpec with WithFakeApplication {
  
  "Reporting a problem" should {

    "return 200 and a valid html page for a valid request and js is not enabled for an unauthenticated user" in new ProblemReportsControllerApplication(fakeApplication) {

        hrmcConnectorWillReturnTheTicketId

        val result = controller.submit()(request)

        status(result) should be(200)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid html page for a valid request and js is not enabled for an authenticated user" in new ProblemReportsControllerApplication(fakeApplication) {
        when(hmrcDeskproConnector.createDeskProTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]](), meq(enrolments), meq(None), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(TicketId(123)))

        val result = controller.submit()(request.withSession(SessionKeys.authToken -> "authToken"))

        status(result) should be(200)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid json for a valid request and js is enabled" in new ProblemReportsControllerApplication(fakeApplication) {
        when(hmrcDeskproConnector.createDeskProTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(true), any[Request[AnyRef]](), meq(None), meq(None), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(TicketId(123)))

        val result = controller.submit()(generateRequest())


        status(result) should be(200)

        val message = contentAsJson(result).\("message").as[String]
        contentAsJson(result).\("status").as[String] shouldBe "OK"
        message should include("<h2 id=\"feedback-thank-you-header\">Thank you</h2>")
        message should include("Someone will get back to you within 2 working days.")
    }

    "return 200 and a valid html page with validation error for invalid input and js is not enabled" in new ProblemReportsControllerApplication(fakeApplication) {
        val result = controller.submit()(generateInvalidRequest(javascriptEnabled = false))

        status(result) should be(200)
        verifyZeroInteractions(hmrcDeskproConnector)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementsByClass("error-notification").size() should be > 0
    }

    "return 200 and a valid json containing validation errors for invalid input and js is enabled" in new ProblemReportsControllerApplication(fakeApplication) {

      val result = controller.submit()(generateInvalidRequest())

      status(result) should be(400)
      verifyZeroInteractions(hmrcDeskproConnector)

      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }

    "fail if the email has invalid syntax (for DeskPRO)" in new ProblemReportsControllerApplication(fakeApplication) {
        val submit = controller.submit()(generateRequest(javascriptEnabled = false, email = "a@a"))
        val page = Jsoup.parse(contentAsString(submit))

        status(submit) shouldBe 200
        verifyZeroInteractions(hmrcDeskproConnector)

        page.getElementsByClass("error-notification").size() should be > 0
    }

    "fail if the name has invalid characters - Javascript disabled" in new ProblemReportsControllerApplication(fakeApplication) {
        val submit = controller.submit()(generateRequest(javascriptEnabled = false, name ="""<a href="blah.com">something</a>""").withSession(SessionKeys.authToken -> "authToken"))
        val page = Jsoup.parse(contentAsString(submit))

        status(submit) shouldBe 200
        verifyZeroInteractions(hmrcDeskproConnector)

        page.getElementsByClass("error-notification").size() should be > 0
    }

    "return error page if the Deskpro ticket creation fails - Javascript disabled" in new ProblemReportsControllerApplication(fakeApplication) {
        when(hmrcDeskproConnector.createDeskProTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]](), meq(None), meq(None), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.failed(new Exception("failed")))

        val result = controller.submit()(request)
        status(result) should be(200)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("report-confirmation-no-data") should not be null
        document.text() should include("Please try again later.")
    }

  }
}

class ProblemReportsControllerApplication(app: Application) extends MockitoSugar {

  val authConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(Json.parse("{ \"allEnrolments\" : []}").as[A](retrieval.reads))
    }
  }

  val controller = new ProblemReportsController(mock[HmrcDeskproConnector], authConnector)(new CFConfig(Environment.simple(), app.configuration), app.injector.instanceOf[MessagesApi])

  val deskproName: String = "John Densmore"
  val deskproEmail: String = "name@mail.com"
  val deskproSubject: String = "Support Request"
  val deskproProblemMessage: String = controller.problemMessage("Some Action", "Some Error")
  val deskproReferrer: String = "/contact/problem_reports"

  val hmrcDeskproConnector = controller.hmrcDeskproConnector

  val enrolments = Some(Enrolments(Set()))

  def generateRequest(javascriptEnabled: Boolean = true, name: String = deskproName, email: String = deskproEmail) = {

    val headers = Seq(("referer", deskproReferrer), ("User-Agent", "iAmAUserAgent")) ++ Seq(("X-Requested-With", "XMLHttpRequest")).filter(_ => javascriptEnabled)

    FakeRequest()
      .withHeaders(headers : _*)
      .withFormUrlEncodedBody("report-name" -> name, "report-email" -> email,
        "report-action" -> "Some Action", "report-error" -> "Some Error", "isJavascript" -> javascriptEnabled.toString)
  }

  def generateInvalidRequest(javascriptEnabled: Boolean = true) = {

    val headers = Seq(("referer", deskproReferrer), ("User-Agent", "iAmAUserAgent")) ++ Seq(("X-Requested-With", "XMLHttpRequest")).filter(_ => javascriptEnabled)

    FakeRequest()
      .withHeaders(headers : _*)
      .withFormUrlEncodedBody("isJavascript" -> javascriptEnabled.toString)
  }

  val request = generateRequest(javascriptEnabled = false)

  def hmrcConnectorWillFail = mockHmrcConnector(Future.failed(new Exception("failed")))

  def hrmcConnectorWillReturnTheTicketId = mockHmrcConnector(Future.successful(TicketId(123)))

  private def mockHmrcConnector(result: Future[TicketId]) = {
    when(hmrcDeskproConnector.createDeskProTicket(
      meq(deskproName),
      meq(deskproEmail),
      meq(deskproSubject),
      meq(deskproProblemMessage),
      meq(deskproReferrer),
      meq(false),
      any[Request[AnyRef]](),
      meq(None),
      meq(None),
      meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(result)
  }
}

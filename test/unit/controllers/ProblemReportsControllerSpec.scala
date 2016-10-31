package controllers

import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Request
import play.api.test.{FakeApplication, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class ProblemReportsControllerSpec extends UnitSpec with WithFakeApplication {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map(
    "govuk-tax.Test.assets.url" -> "",
    "govuk-tax.Test.assets.version" -> "",
    "govuk-tax.Test.google-analytics.token" -> "",
    "govuk-tax.Test.google-analytics.host" -> ""
  ))

  "Reporting a problem" should {

    "return 200 and a valid html page for a valid request and js is not enabled for an unauthenticated user" in new ProblemReportsControllerApplication {

      hrmcConnectorWillReturnTheTicketId

      val result = controller.doReport()(request)

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }


    "return 200 and a valid html page for a valid request and js is not enabled for an authenticated user" in new ProblemReportsControllerApplication {
      when(authConnector.currentAuthority(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(Authority("uri", Accounts(), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, None, None))))
      when(hmrcDeskproConnector.createDeskProTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]](), meq(accounts), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(TicketId(123)))

      val result = controller.doReport()(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation") should not be null
    }

    "return 200 and a valid json for a valid request and js is enabled" in new ProblemReportsControllerApplication {
      when(hmrcDeskproConnector.createDeskProTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(true), any[Request[AnyRef]](), meq(None), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(TicketId(123)))

      val result = controller.doReport()(generateRequest())

      status(result) should be(200)

      val message = contentAsJson(result).\("message").as[String]
      contentAsJson(result).\("status").as[String] shouldBe "OK"
      message should include("<h2 id=\"feedback-thank-you-header\">Thank you</h2>")
      message should include("Someone will get back to you within 2 working days.")
    }

    "return 200 and a valid html page for invalid input and js is not enabled" in new ProblemReportsControllerApplication {

      val result = controller.doReport()(generateInvalidRequest(javascriptEnabled = false))

      status(result) should be(200)
      verifyZeroInteractions(hmrcDeskproConnector)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
    }

    "return 400 and a valid json for invalid input and js is enabled" in new ProblemReportsControllerApplication {

      val result = controller.doReport()(generateInvalidRequest())

      status(result) should be(400)
      verifyZeroInteractions(hmrcDeskproConnector)

      contentAsJson(result).\("status").as[String] shouldBe "ERROR"
    }

    "fail if the email has invalid syntax (for DeskPRO)" in new ProblemReportsControllerApplication {
      val submit = controller.doReport()(generateRequest(javascriptEnabled = false, email = "a@a"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 200
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementById("report-confirmation-no-data") should not be null
    }

    "fail if the name has invalid characters - Javascript disabled" in new ProblemReportsControllerApplication {
      when(authConnector.currentAuthority(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.successful(Some(Authority("uri",  Accounts(), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, None, None))))

      val submit = controller.doReport()(generateRequest(javascriptEnabled = false, name="""<a href="blah.com">something</a>""").withSession(SessionKeys.authToken -> "authToken"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 200
      verifyZeroInteractions(hmrcDeskproConnector)

      page.getElementById("report-confirmation-no-data") should not be null
    }

    "Return error page if the Deskpro ticket creation fails - Javascript disabled" in new ProblemReportsControllerApplication {
      when(hmrcDeskproConnector.createDeskProTicket(meq("John Densmore"), meq("name@mail.com"), meq("Support Request"), meq(controller.problemMessage("Some Action", "Some Error")), meq("/contact/problem_reports"), meq(false), any[Request[AnyRef]](), meq(None), meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(Future.failed(new Exception("failed")))

      val result = controller.doReport()(request)
      status(result) should be(200)

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("report-confirmation-no-data") should not be null
      document.text() should include("Please try again later.")
    }

    "render the thank you message given" in new ProblemReportsControllerApplication {
      hrmcConnectorWillReturnTheTicketId

      val submit = controller.doReport(Some("common.feedback.title"))(request)
      val page = Jsoup.parse(contentAsString(submit))

      page.text() should include("Get help using this service")
    }

    "render the default thank you message if one is not provided" in new ProblemReportsControllerApplication {
      hrmcConnectorWillReturnTheTicketId

      val submit = controller.doReport()(request)
      val page = Jsoup.parse(contentAsString(submit))

      page.text() should include("Someone will get back to you within 2 working days.")
    }

  }

}

class ProblemReportsControllerApplication extends MockitoSugar {

  val controller = new ProblemReportsController {
    override lazy val hmrcDeskproConnector = mock[HmrcDeskproConnector]
    override lazy val authConnector = mock[AuthConnector]
  }

  val deskproName: String = "John Densmore"
  val deskproEmail: String = "name@mail.com"
  val deskproSubject: String = "Support Request"
  val deskproProblemMessage: String = controller.problemMessage("Some Action", "Some Error")
  val deskproReferrer: String = "/contact/problem_reports"

  val hmrcDeskproConnector = controller.hmrcDeskproConnector
  val authConnector = controller.authConnector

  val accounts = Some(Accounts())

  def generateRequest(javascriptEnabled: Boolean = true, name:String = deskproName, email: String = deskproEmail) = FakeRequest()
    .withHeaders(("referer", deskproReferrer), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("report-name" -> name, "report-email" -> email,
      "report-action" -> "Some Action", "report-error" -> "Some Error", "isJavascript" -> javascriptEnabled.toString)

  def generateInvalidRequest(javascriptEnabled: Boolean = true) = FakeRequest()
    .withHeaders(("referer", deskproReferrer), ("User-Agent", "iAmAUserAgent"))
    .withFormUrlEncodedBody("isJavascript" -> javascriptEnabled.toString)

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
      meq(None))(Matchers.any(classOf[HeaderCarrier]))).thenReturn(result)
  }

}

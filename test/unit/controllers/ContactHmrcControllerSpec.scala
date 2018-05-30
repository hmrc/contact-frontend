package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.CFConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatest.mockito.MockitoSugar
import play.api.Environment
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.jsoup.Jsoup

import scala.concurrent.Future
import scala.concurrent.duration._

class ContactHmrcControllerSpec
  extends UnitSpec
    with GivenWhenThen
    with MockitoSugar
    with WithFakeApplication {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: akka.util.Timeout = 10.seconds

  private def mockDeskproConnector(result: Future[TicketId]): HmrcDeskproConnector = {
    val hmrcDeskproConnector = mock[HmrcDeskproConnector]
    when(hmrcDeskproConnector.createDeskProTicket(
      any[String],
      any[String],
      any[String],
      any[String],
      any[String],
      any[Boolean],
      any[Request[AnyRef]](),
      any[Option[Enrolments]],
      any[Option[String]])(any[HeaderCarrier])
    ).thenReturn(result)

    hmrcDeskproConnector
  }

  "ContactHmrcController" should {

    "return expected OK for non-authenticated index page" in {
      Given("a GET request")
      val authConnector = mock[AuthConnector]

      val configuration = fakeApplication.configuration
      val environment = Environment.simple()

      val appConfig = new CFConfig(environment, configuration)
      val messages = fakeApplication.injector.instanceOf[MessagesApi]

      val controller =
        new ContactHmrcController(
          mockDeskproConnector(Future.successful(TicketId(12345))),
          authConnector,
          configuration,
          environment)(appConfig, messages)

      val contactRequest = FakeRequest().withHeaders(("Referer", "/some-service-page"))
      val serviceName = "my-fake-service"

      When("the unauthenticated Contact HMRC page is requested with a service name")
      val contactResult = controller.indexUnauthenticated(serviceName)(contactRequest)

      Then("the expected page should be returned")
      status(contactResult) shouldBe 200

      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referer").attr("value") shouldBe "/some-service-page"
    }

    "return expected OK for non-authenticated submit page" in {
      Given("a POST request containing a valid form")
      val deskproConnector = mockDeskproConnector(Future.successful(TicketId(12345)))
      val authConnector = mock[AuthConnector]

      val configuration = fakeApplication.configuration
      val environment = Environment.simple()

      val appConfig = new CFConfig(environment, configuration)
      val messages = fakeApplication.injector.instanceOf[MessagesApi]

      val controller =
        new ContactHmrcController(deskproConnector,
          authConnector,
          configuration,
          environment)(appConfig, messages)

      val fields = Map(
        "contact-name" -> "Bob The Builder",
        "contact-email" -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript" -> "false",
        "referer" -> "n/a",
        "csrfToken" -> "n/a",
        "service" -> "scp"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to unauthenticated submit contact page")
      val submitResult = controller.submitUnauthenticated(contactRequest)

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(deskproConnector)
        .createDeskProTicket(any[String],
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]])(any[HeaderCarrier])

      Then("the user is redirected to the thanks page")
      status(submitResult) shouldBe 303
      redirectLocation(submitResult).get shouldBe "/contact/contact-hmrc/thanks-unauthenticated"
    }

    "return expected Internal Error when hmrc-deskpro errors for non-authenticated submit page" in {
      Given("a POST request containing a valid form")
      val deskproConnector = mockDeskproConnector(
        Future.failed(new Exception("This is an expected test error")))
      val authConnector = mock[AuthConnector]

      val configuration = fakeApplication.configuration
      val environment = Environment.simple()

      val appConfig = new CFConfig(environment, configuration)
      val messages = fakeApplication.injector.instanceOf[MessagesApi]

      val controller =
        new ContactHmrcController(deskproConnector,
          authConnector,
          configuration,
          environment)(appConfig, messages)

      val fields = Map(
        "contact-name" -> "Bob The Builder",
        "contact-email" -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript" -> "false",
        "referer" -> "n/a",
        "csrfToken" -> "n/a",
        "service" -> "scp"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to unauthenticated submit contact page")
      val submitResult = controller.submitUnauthenticated(contactRequest)

      And("the Deskpro connector fails")

      Then("the user is sent to an error page")
      status(submitResult) shouldBe 500
    }

    "return expected OK for non-authenticated thanks page" in {
      Given("a GET request")
      val authConnector = mock[AuthConnector]

      val configuration = fakeApplication.configuration
      val environment = Environment.simple()

      val appConfig = new CFConfig(environment, configuration)
      val messages = fakeApplication.injector.instanceOf[MessagesApi]

      val controller =
        new ContactHmrcController(
          mockDeskproConnector(Future.successful(TicketId(12345))),
          authConnector,
          configuration,
          environment)(appConfig, messages)

      val contactRequest = FakeRequest().withSession(("ticketId", "12345"))

      When("the unauthenticated Thanks for contacting HMRC page is requested")
      val thanksResult = controller.thanksUnauthenticated(contactRequest)

      Then("the expected page should be returned")
      status(thanksResult) shouldBe 200
    }
  }
}

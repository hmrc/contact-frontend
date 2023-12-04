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

import akka.actor.ActorSystem
import config.CFConfig
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.{TicketConstants, TicketId}
import connectors.enrolments.EnrolmentsConnector
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.verification.VerificationWithTimeout
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import util.RefererHeaderRetriever

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class ContactHmrcControllerSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with MockitoSugar
    with Eventually
    with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "useRefererHeader" -> true)
      .build()

  implicit val actorSystem: ActorSystem = ActorSystem()

  implicit val message: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  val connectorTimeout: VerificationWithTimeout =
    Mockito.timeout(5000)

  "ContactHmrcController" should {

    "return expected OK for index page with no URL parameters" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))

      When("the page is requested with a service name via headers")
      val contactResult =
        controller.index(None, None, None)(contactRequest)

      Then("the expected page should be returned")
      status(contactResult) shouldBe 200

      val page = Jsoup.parse(contentAsString(contactResult))
      page
        .body()
        .select("form[id=contact-hmrc-form]")
        .first
        .attr("action")                                        shouldBe s"/contact/contact-hmrc"
      page.body().select("input[name=referrer]").attr("value") shouldBe "/some-service-page"
      page.body().select("input[name=service]").attr("value")  shouldBe ""
    }

    "return expected OK for index page with URL parameters" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))
      val serviceName    = Some("my-fake-service")

      When("the page is requested with a service name via URL")
      val contactResult =
        controller.index(serviceName, Some("my-action"), Some("/url-from-query"))(contactRequest)

      Then("the expected page should be returned")
      status(contactResult) shouldBe 200

      val page        = Jsoup.parse(contentAsString(contactResult))
      val queryString = s"service=my-fake-service&userAction=my-action&referrerUrl=%2Furl-from-query"
      page
        .body()
        .select("form[id=contact-hmrc-form]")
        .first
        .attr("action")                                shouldBe s"/contact/contact-hmrc?$queryString"
      page.body().select("input[name=referrer]").`val` shouldBe "/url-from-query"
      page.body().select("input[name=service]").`val`  shouldBe "my-fake-service"
    }

    "use the referrerUrl parameter if supplied" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))
      val serviceName    = Some("my-fake-service")
      val referrerUrl    = Some("https://www.example.com/some-service")

      When("the page is requested with a service name and a referrer url")
      val contactResult = controller.index(serviceName, None, referrerUrl)(contactRequest)

      Then("the referrer hidden input should contain that value")
      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().select("input[name=referrer]").`val` shouldBe "https://www.example.com/some-service"
    }

    "fallback to n/a if no referrer information is available" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest()
      val serviceName    = Some("my-fake-service")

      When("the page is requested with a service name")
      val contactResult = controller.index(serviceName, None, None)(contactRequest)

      Then("the referrer hidden input should be n/a")
      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().select("input[name=referrer]").`val` shouldBe "n/a"
    }

    "return expected OK for submit page" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "n/a",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/some-service-page"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to submit contact page")
      val submitResult = controller.submit(None, None, None)(contactRequest)

      Then("the user is redirected to the thanks page")
      status(submitResult)               shouldBe 303
      redirectLocation(submitResult).get shouldBe "/contact/contact-hmrc/thanks"

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(ticketQueueConnector, connectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[TicketConstants]
        )(any[HeaderCarrier])
    }

    "send the referrer URL to DeskPro" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> ""
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to submit contact page")
      controller.submit(None, None, None)(contactRequest)

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(ticketQueueConnector, connectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          meq[String]("https://www.other-gov-domain.gov.uk/path/to/service/page"),
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[TicketConstants]
        )(any[HeaderCarrier])
    }

    "send the referrer information to DeskPro with userAction replacing the path if non-empty" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/overridden/path"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to submit contact page")
      controller.submit(None, None, None)(contactRequest)

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(ticketQueueConnector, connectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          meq[String]("https://www.other-gov-domain.gov.uk/overridden/path"),
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[TicketConstants]
        )(any[HeaderCarrier])
    }

    "display errors when form isn't filled out at all" in new TestScope {

      val fields = Map(
        "contact-name"     -> "",
        "contact-email"    -> "",
        "contact-comments" -> "",
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/overridden/path"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submit(None, None, None)(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      val errors   = document.select(".govuk-error-message").asScala
      errors.length should be(3)

      document.title() should be("Error: " + Messages("contact.title"))

      errors.exists(_.text().contains(Messages("contact.comments.error.required"))) shouldBe true
      errors.exists(_.text().contains(Messages("contact.name.error.required")))     shouldBe true
      errors.exists(_.text().contains(Messages("contact.email.error.required")))    shouldBe true
    }

    "display error messages when comments size exceeds limit" in new TestScope {
      val msg2500 = "x" * 2500

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> msg2500,
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/overridden/path"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submit(None, None, None)(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("contact.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("contact.comments.error.length"))) shouldBe true
    }

    "display error messages when email is invalid" in new TestScope {
      val badEmail = "firstname'email.gov."

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> badEmail,
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/overridden/path"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submit(None, None, None)(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("contact.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("contact.email.error.invalid"))) shouldBe true
    }

    "display error messages when email is too long" in new TestScope {
      val tooLongEmail = ("x" * 64) + "@" + ("x" * 63) + "." + ("x" * 63) + "." + ("x" * 63) + "." + ("x" * 57) + ".com"

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> tooLongEmail,
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/overridden/path"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submit(None, None, None)(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("contact.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.exists(_.text().contains(Messages("contact.email.error.length"))) shouldBe true
    }

    "display error messages when name is too long" in new TestScope {
      val longName = "x" * 256

      val fields         = Map(
        "contact-name"     -> longName,
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "userAction"       -> "/overridden/path"
      )
      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submit(None, None, None)(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("contact.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.exists(_.text().contains(Messages("contact.name.error.length"))) shouldBe true
    }

    "return expected Internal Error when backend service errors for submit page" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.failed(new Exception("This is an expected test error")))

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "n/a",
        "csrfToken"        -> "n/a",
        "service"          -> "scp"
      )

      val contactRequest = FakeRequest("POST", "/").withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to submit contact page")
      val submitResult = controller.submit(None, None, None)(contactRequest)

      And("the Deskpro connector fails")

      Then("the user is sent to an error page")
      status(submitResult) shouldBe 500
    }

    "return expected OK for thanks page" in new TestScope {
      Given("a GET request")

      val contactRequest = FakeRequest().withSession(("ticketId", "12345"))

      When("the Thanks for contacting HMRC page is requested")
      val thanksResult = controller.thanks(contactRequest)

      Then("the expected page should be returned")
      status(thanksResult) shouldBe 200
    }
  }

  class TestScope extends MockitoSugar {

    val configuration = app.configuration

    implicit val appConfig: CFConfig                = new CFConfig(configuration)
    implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
    implicit val messages: MessagesApi              = app.injector.instanceOf[MessagesApi]

    val ticketQueueConnector                     = mock[DeskproTicketQueueConnector]
    val enrolmentsConnector: EnrolmentsConnector = mock[EnrolmentsConnector]
    when(enrolmentsConnector.maybeAuthenticatedUserEnrolments()(any(), any())).thenReturn(Future.successful(None))

    val errorPage          = app.injector.instanceOf[views.html.InternalErrorPage]
    val pfContactPage      = app.injector.instanceOf[views.html.ContactHmrcPage]
    val pfConfirmationPage = app.injector.instanceOf[views.html.ContactHmrcConfirmationPage]

    val controller =
      new ContactHmrcController(
        ticketQueueConnector,
        enrolmentsConnector,
        Stubs.stubMessagesControllerComponents(messagesApi = messages),
        errorPage,
        pfContactPage,
        pfConfirmationPage,
        new RefererHeaderRetriever
      )

    def mockDeskproConnector(result: Future[TicketId]): Unit =
      when(
        ticketQueueConnector.createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[TicketConstants]
        )(any[HeaderCarrier])
      ).thenReturn(result)

  }

}

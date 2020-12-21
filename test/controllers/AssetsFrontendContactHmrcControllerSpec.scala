/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.CFConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Matchers.{eq => mockitoEq}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.verification.VerificationWithTimeout
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, contentAsString, redirectLocation, _}
import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs
import collection.JavaConverters._

import scala.concurrent.{ExecutionContext, Future}

class AssetsFrontendContactHmrcControllerSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with MockitoSugar
    with Eventually
    with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "enablePlayFrontendContactHmrcForm" -> false)
      .build()

  implicit val actorSystem: ActorSystem        = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val message: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  val hmrcDeskproConnectorTimeout: VerificationWithTimeout =
    Mockito.timeout(5000)

  "ContactHmrcController" should {

    "return expected OK for non-authenticated index page" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))
      val serviceName    = Some("my-fake-service")

      When("the unauthenticated Contact HMRC page is requested with a service name")
      val contactResult = controller.indexUnauthenticated(serviceName, None, None)(contactRequest)

      Then("the expected page should be returned")
      status(contactResult) shouldBe 200

      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referrer").attr("value") shouldBe "/some-service-page"
      page.body().getElementById("service").attr("value")  shouldBe "my-fake-service"
    }

    "use the referrerUrl parameter if supplied" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))
      val serviceName    = Some("my-fake-service")
      val referrerUrl    = Some("https://www.example.com/some-service")

      When("the unauthenticated Contact HMRC page is requested with a service name and a referrer url")
      val contactResult = controller.indexUnauthenticated(serviceName, None, referrerUrl)(contactRequest)

      Then("the referrer hidden input should contain that value")
      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referrer").attr("value") shouldBe "https://www.example.com/some-service"
    }

    "fallback to n/a if no referrer information is available" in new TestScope {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest()
      val serviceName    = Some("my-fake-service")

      When("the unauthenticated Contact HMRC page is requested with a service name")
      val contactResult = controller.indexUnauthenticated(serviceName, None, None)(contactRequest)

      Then("the referrer hidden input should be n/a")
      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referrer").attr("value") shouldBe "n/a"
    }

    "return expected OK for non-authenticated submit page" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> "bob@build-it.com",
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "n/a",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/some-service-page"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to unauthenticated submit contact page")
      val submitResult = controller.submitUnauthenticated(contactRequest)

      Then("the user is redirected to the thanks page")
      status(submitResult)               shouldBe 303
      redirectLocation(submitResult).get shouldBe "/contact/contact-hmrc/thanks-unauthenticated"

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(hmrcDeskproConnector, hmrcDeskproConnectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
    }

    "send the referrer URL to DeskPro" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> "bob@build-it.com",
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> ""
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to unauthenticated submit contact page")
      controller.submitUnauthenticated(contactRequest)

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(hmrcDeskproConnector, hmrcDeskproConnectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          mockitoEq[String]("https://www.other-gov-domain.gov.uk/path/to/service/page"),
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
    }

    "send the referrer information to DeskPro with userAction replacing the path if non-empty" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> "bob@build-it.com",
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/overridden/path"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to unauthenticated submit contact page")
      controller.submitUnauthenticated(contactRequest)

      Then("the message is sent to the Deskpro connector")
      Mockito
        .verify(hmrcDeskproConnector, hmrcDeskproConnectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          mockitoEq[String]("https://www.other-gov-domain.gov.uk/overridden/path"),
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
    }

    "display errors when form isn't filled out at all" in new TestScope {

      val fields = Map(
        "contact-name"          -> "",
        "contact-email"         -> "",
        "contact-comments"      -> "",
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/overridden/path"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submitUnauthenticated()(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      val errors   = document.getElementsByClass("error-message").asScala
      errors.length should be(3)

      document.title() should be(Messages("contact.heading"))

      errors.exists(_.text().contains(Messages("contact.comments.error.required"))) shouldBe true
      errors.exists(_.text().contains(Messages("contact.name.error.required")))     shouldBe true
      errors.exists(_.text().contains(Messages("contact.email.error.required")))    shouldBe true
    }

    "display error messages when comments size exceeds limit" in new TestScope {
      val msg2500 = "x" * 2500

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> "bob@build-it.com",
        "contact-comments"      -> msg2500,
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/overridden/path"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submitUnauthenticated()(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be(Messages("contact.heading"))
      val errors = document.getElementsByClass("error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("contact.comments.error.length"))) shouldBe true
    }

    "display error messages when email is invalid" in new TestScope {
      val badEmail = "firstname'email.gov."

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> badEmail,
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/overridden/path"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submitUnauthenticated()(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be(Messages("contact.heading"))
      val errors = document.getElementsByClass("error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("contact.email.error.invalid"))) shouldBe true
    }

    "display error messages when email is too long" in new TestScope {
      val tooLongEmail = ("x" * 64) + "@" + ("x" * 63) + "." + ("x" * 63) + "." + ("x" * 63) + "." + ("x" * 57) + ".com"

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> tooLongEmail,
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/overridden/path"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submitUnauthenticated()(contactRequest)

      status(result) should be(400)

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be(Messages("contact.heading"))
      val errors = document.getElementsByClass("error-message").asScala
      errors.exists(_.text().contains(Messages("contact.email.error.length"))) shouldBe true
    }

    "display error messages when name is too long" in new TestScope {
      val longName = "x" * 256

      val fields         = Map(
        "contact-name"          -> longName,
        "contact-email"         -> "bob@build-it.com",
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "https://www.other-gov-domain.gov.uk/path/to/service/page",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "abFeatures"            -> "GetHelpWithThisPageFeature_A",
        "recaptcha-v3-response" -> "xx",
        "userAction"            -> "/overridden/path"
      )
      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)
      val result         = controller.submitUnauthenticated()(contactRequest)

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be(Messages("contact.heading"))
      val errors = document.getElementsByClass("error-message").asScala
      errors.exists(_.text().contains(Messages("contact.name.error.length"))) shouldBe true
    }

    "return expected Internal Error when hmrc-deskpro errors for non-authenticated submit page" in new TestScope {
      Given("a POST request containing a valid form")
      mockDeskproConnector(Future.failed(new Exception("This is an expected test error")))

      val fields = Map(
        "contact-name"          -> "Bob The Builder",
        "contact-email"         -> "bob@build-it.com",
        "contact-comments"      -> "Can We Fix It?",
        "isJavascript"          -> "false",
        "referrer"              -> "n/a",
        "csrfToken"             -> "n/a",
        "service"               -> "scp",
        "recaptcha-v3-response" -> "xx"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      When("the request is POSTed to unauthenticated submit contact page")
      val submitResult = controller.submitUnauthenticated(contactRequest)

      And("the Deskpro connector fails")

      Then("the user is sent to an error page")
      status(submitResult) shouldBe 500
    }

    "return expected OK for non-authenticated thanks page" in new TestScope {
      Given("a GET request")

      val contactRequest = FakeRequest().withSession(("ticketId", "12345"))

      When("the unauthenticated Thanks for contacting HMRC page is requested")
      val thanksResult = controller.thanksUnauthenticated(contactRequest)

      Then("the expected page should be returned")
      status(thanksResult) shouldBe 200
    }
  }

  "Submitting contact hrmc form" should {
    "return JSON with ticket id for sucessful form submission" in new TestScope {
      Given("we have a valid reqest")

      val ticketId = TicketId(12345)

      mockDeskproConnector(Future.successful(ticketId))

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "n/a",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "abFeatures"       -> "GetHelpWithThisPageFeature_A"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      val resubmitUrl = "/contact-frontend/form"

      When("we submit the request")
      val result =
        controller.submitContactHmrcPartialForm(resubmitUrl = resubmitUrl, renderFormOnly = false)(contactRequest)

      Then("ticket is sent to deskpro")
      Mockito
        .verify(hmrcDeskproConnector, hmrcDeskproConnectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])

      And("ticket id is returned to the user")
      status(result) shouldBe 200
      val resultAsJson = contentAsJson(result)
      resultAsJson.as[Int] shouldBe ticketId.ticket_id
    }

    "redisplay form in case of validation errors - rendering only form without header" in new TestScope {
      Given("we have an invalid request")

      val fields = Map.empty

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      val resubmitUrl = "/contact-frontend/form"

      When("we submit the request")
      val result =
        controller.submitContactHmrcPartialForm(resubmitUrl = resubmitUrl, renderFormOnly = true)(contactRequest)

      Then("ticket is not sent to deskpro")
      Mockito.verifyZeroInteractions(hmrcDeskproConnector)

      And("an error message is returned")
      status(result) shouldBe 400
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-message") shouldNot be(empty)

      And("a header shouldn't be visible")
      page.body().getElementsByClass("page-header") should be(empty)
    }

    "redisplay form in case of validation errors - rendering with header" in new TestScope {
      Given("we have an invalid request")

      val fields = Map.empty

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      val resubmitUrl = "/contact-frontend/form"

      When("we submit the request")
      val result =
        controller.submitContactHmrcPartialForm(resubmitUrl = resubmitUrl, renderFormOnly = false)(contactRequest)

      Then("ticket is not sent to deskpro")
      Mockito.verifyZeroInteractions(hmrcDeskproConnector)

      And("an error message is returned")
      status(result) shouldBe 400
      val page = Jsoup.parse(contentAsString(result))
      page.body().getElementsByClass("error-message") shouldNot be(empty)

      And("a header shouldn't be visible")
      page.body().getElementsByClass("page-header") shouldNot be(empty)
    }

    "show an error page with bad request HTTP code if sending the data to deskpro failed" in new TestScope {
      Given("we have a valid reqest")

      val fields = Map(
        "contact-name"     -> "Bob The Builder",
        "contact-email"    -> "bob@build-it.com",
        "contact-comments" -> "Can We Fix It?",
        "isJavascript"     -> "false",
        "referrer"         -> "n/a",
        "csrfToken"        -> "n/a",
        "service"          -> "scp",
        "abFeatures"       -> "GetHelpWithThisPageFeature_A"
      )

      val contactRequest = FakeRequest().withFormUrlEncodedBody(fields.toSeq: _*)

      val resubmitUrl = "/contact-frontend/form"

      And("Deskpro doesn't work properly")
      mockDeskproConnector(Future.failed(new Exception("Expected exception")))

      When("we submit the request")
      val result =
        controller.submitContactHmrcPartialForm(resubmitUrl = resubmitUrl, renderFormOnly = false)(contactRequest)

      Then("ticket is sent to deskpro")
      Mockito
        .verify(hmrcDeskproConnector, hmrcDeskproConnectorTimeout)
        .createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])

      And("an error message is returned to the user")
      status(result) shouldBe 500
      val page = Jsoup.parse(contentAsString(result))
      page.body().select("h1").first.text() shouldBe "Sorry, we’re experiencing technical difficulties"
    }
  }

  class TestScope extends MockitoSugar {

    val authConnector = mock[AuthConnector]

    val configuration = app.configuration

    implicit val appConfig        = new CFConfig(configuration)
    implicit val executionContext = ExecutionContext.Implicits.global
    implicit val messages         = app.injector.instanceOf[MessagesApi]

    val hmrcDeskproConnector = mock[HmrcDeskproConnector]

    val contactPage             = app.injector.instanceOf[views.html.contact_hmrc]
    val contactConfirmationPage = app.injector.instanceOf[views.html.contact_hmrc_confirmation]
    val contactForm             = app.injector.instanceOf[views.html.partials.contact_hmrc_form]
    val contactFormConfirmation =
      app.injector.instanceOf[views.html.partials.contact_hmrc_form_confirmation]
    val deskproErrorPage        = app.injector.instanceOf[views.html.DeskproErrorPage]
    val pfContactPage           = app.injector.instanceOf[views.html.ContactHmrcPage]
    val pfConfirmationPage      = app.injector.instanceOf[views.html.ContactHmrcConfirmationPage]

    val controller =
      new ContactHmrcController(
        hmrcDeskproConnector,
        authConnector,
        configuration,
        Stubs.stubMessagesControllerComponents(messagesApi = messages),
        contactPage,
        contactConfirmationPage,
        contactForm,
        contactFormConfirmation,
        deskproErrorPage,
        pfContactPage,
        pfConfirmationPage
      )

    def mockDeskproConnector(result: Future[TicketId]): Unit =
      when(
        hmrcDeskproConnector.createDeskProTicket(
          any[String],
          any[String],
          any[String],
          any[String],
          any[String],
          any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
      ).thenReturn(result)

  }

}

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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, contentAsString, redirectLocation, _}
import services.CaptchaService
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.{ExecutionContext, Future}

class ContactHmrcControllerSpec
    extends AnyWordSpec
    with GivenWhenThen
    with Matchers
    with MockitoSugar
    with Eventually
    with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false)
      .build()

  implicit val actorSystem: ActorSystem        = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "ContactHmrcController" should {

    "return expected OK for non-authenticated index page" in new ContactHmrcControllerApplication {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))
      val serviceName    = "my-fake-service"

      When("the unauthenticated Contact HMRC page is requested with a service name")
      val contactResult = controller.indexUnauthenticated(serviceName, None, None)(contactRequest)

      Then("the expected page should be returned")
      status(contactResult) shouldBe 200

      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referrer").attr("value") shouldBe "/some-service-page"
      page.body().getElementById("service").attr("value")  shouldBe "my-fake-service"
    }

    "use the referrerUrl parameter if supplied" in new ContactHmrcControllerApplication {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest().withHeaders((REFERER, "/some-service-page"))
      val serviceName    = "my-fake-service"
      val referrerUrl    = Some("https://www.example.com/some-service")

      When("the unauthenticated Contact HMRC page is requested with a service name and a referrer url")
      val contactResult = controller.indexUnauthenticated(serviceName, None, referrerUrl)(contactRequest)

      Then("the referrer hidden input should contain that value")
      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referrer").attr("value") shouldBe "https://www.example.com/some-service"
    }

    "fallback to n/a if no referrer information is available" in new ContactHmrcControllerApplication {
      Given("a GET request")
      mockDeskproConnector(Future.successful(TicketId(12345)))

      val contactRequest = FakeRequest()
      val serviceName    = "my-fake-service"

      When("the unauthenticated Contact HMRC page is requested with a service name")
      val contactResult = controller.indexUnauthenticated(serviceName, None, None)(contactRequest)

      Then("the referrer hidden input should be n/a")
      val page = Jsoup.parse(contentAsString(contactResult))
      page.body().getElementById("referrer").attr("value") shouldBe "n/a"
    }

    "return expected OK for non-authenticated submit page" in new ContactHmrcControllerApplication {
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
        .verify(hmrcDeskproConnector)
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

    "send the referrer URL to DeskPro" in new ContactHmrcControllerApplication {
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
        .verify(hmrcDeskproConnector)
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

    "send the referrer information to DeskPro with userAction replacing the path if non-empty" in new ContactHmrcControllerApplication {
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
        .verify(hmrcDeskproConnector)
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

    "return expected Internal Error when hmrc-deskpro errors for non-authenticated submit page" in new ContactHmrcControllerApplication {
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

    "return expected OK for non-authenticated thanks page" in new ContactHmrcControllerApplication {
      Given("a GET request")

      val contactRequest = FakeRequest().withSession(("ticketId", "12345"))

      When("the unauthenticated Thanks for contacting HMRC page is requested")
      val thanksResult = controller.thanksUnauthenticated(contactRequest)

      Then("the expected page should be returned")
      status(thanksResult) shouldBe 200
    }
  }

  "Submitting contact hrmc form" should {
    "return JSON with ticket id for sucessful form submission" in new ContactHmrcControllerApplication {
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
        .verify(hmrcDeskproConnector)
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

    "redisplay form in case of validation errors - rendering only form without header" in new ContactHmrcControllerApplication {
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

    "redisplay form in case of validation errors - rendering with header" in new ContactHmrcControllerApplication {
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

    "show an error page with bad request HTTP code if sending the data to deskpro failed" in new ContactHmrcControllerApplication {
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
        .verify(hmrcDeskproConnector, Mockito.timeout(5000))
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
      page.body().getElementsByClass("page-header").text() shouldBe "Sorry, we’re experiencing technical difficulties"
    }
  }

  class ContactHmrcControllerApplication extends MockitoSugar {

    val authConnector = mock[AuthConnector]

    val configuration = fakeApplication.configuration

    implicit val appConfig        = new CFConfig(configuration)
    implicit val executionContext = ExecutionContext.Implicits.global
    implicit val messages         = fakeApplication.injector.instanceOf[MessagesApi]

    val hmrcDeskproConnector = mock[HmrcDeskproConnector]

    val captchaService = new CaptchaService {
      override def validateCaptcha(response: String)(implicit headerCarrier: HeaderCarrier): Future[Boolean] =
        Future.successful(true)
    }

    val contactPage             = fakeApplication.injector.instanceOf[views.html.contact_hmrc]
    val contactConfirmationPage = fakeApplication.injector.instanceOf[views.html.contact_hmrc_confirmation]
    val contactForm             = fakeApplication.injector.instanceOf[views.html.partials.contact_hmrc_form]
    val contactFormConfirmation =
      fakeApplication.injector.instanceOf[views.html.partials.contact_hmrc_form_confirmation]
    val deskproErrorPage        = fakeApplication.injector.instanceOf[views.html.deskpro_error]
    val recaptcha               = fakeApplication.injector.instanceOf[views.html.helpers.recaptcha]

    val controller =
      new ContactHmrcController(
        hmrcDeskproConnector,
        authConnector,
        captchaService,
        configuration,
        Stubs.stubMessagesControllerComponents(messagesApi = messages),
        contactPage,
        contactConfirmationPage,
        contactForm,
        contactFormConfirmation,
        deskproErrorPage,
        recaptcha
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

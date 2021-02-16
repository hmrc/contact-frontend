/*
 * Copyright 2021 HM Revenue & Customs
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

import config.CFConfig
import connectors.deskpro.HmrcDeskproConnector
import connectors.deskpro.domain.TicketId
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames.REFERER
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.tools.Stubs

import scala.concurrent.{ExecutionContext, Future}

class AccessibilityControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false, "enablePlayFrontendAccessibilityForm" -> true)
      .build()

  implicit val message: Messages = app.injector.instanceOf[MessagesApi].preferred(Seq(Lang("en")))

  // RFC 5321: https://tools.ietf.org/html/rfc5321
  // Maximum domain name length: https://www.nic.ad.jp/timeline/en/20th/appendix1.html#:~:text=Each%20element%20of%20a%20domain,a%20maximum%20of%20253%20characters.
  val tooLongEmail = ("x" * 64) + "@" + ("x" * 63) + "." + ("x" * 63) + "." + ("x" * 63) + "." + ("x" * 57) + ".com"

  "Accessibility form endpoint" should {

    "redirect to unauthenticated page when user is not logged in" in new TestScope {
      val request    = FakeRequest()
      val userAction = "/page/test?1234=xyz"
      val result     = controller.accessibilityForm(service = None, userAction = Some(userAction))(request)

      status(result)           should be(303)
      header(LOCATION, result) should be(
        Some(s"/contact/accessibility-unauthenticated?userAction=%2Fpage%2Ftest%3F1234%3Dxyz")
      )
    }

    "show the authenticated form page if logged in" in new TestScope {
      val request = FakeRequest()
      val result  = controller.unauthenticatedAccessibilityForm(
        service = None,
        userAction = Some("test?1234=xyz"),
        referrerUrl = Some("some.referrer.url")
      )(request)
      status(result)           should be(200)
      header(LOCATION, result) should be(None)
    }
  }

  "Reporting an accessibility problem without logging in" should {

    "return 200 and a valid html page for a request with optional referrerUrl" in new TestScope {

      val request = FakeRequest().withHeaders((REFERER, "referrer.from.header"))
      val result  = controller.unauthenticatedAccessibilityForm(
        service = None,
        userAction = Some("test?1234=xyz"),
        referrerUrl = Some("some.referrer.url")
      )(request)

      status(result) should be(200)
      val document = Jsoup.parse(contentAsString(result))
      document.title()                                                             should be(Messages("accessibility.title"))
      document.getElementById("accessibility-form")                                should not be null
      document.getElementById("service").`val`()                                   should be("")
      document.getElementsByAttributeValue("name", "userAction").first().`val`() shouldBe "test?1234=xyz"
      document.getElementsByAttributeValue("name", "referrer").first().`val`()   shouldBe "some.referrer.url"
    }

    "return 200 and a valid html page for a request when no referrerUrl and referer in header" in new TestScope {

      val request = FakeRequest().withHeaders((REFERER, "referrer.from.header"))
      val result  = controller.unauthenticatedAccessibilityForm(
        service = None,
        userAction = Some("test?1234=xyz"),
        referrerUrl = None
      )(request)

      status(result) should be(200)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttributeValue("name", "referrer").first().`val`() shouldBe "referrer.from.header"
    }

    "return 200 and a valid html page for a request when no referrerUrl and no referer in header" in new TestScope {

      val request = FakeRequest()
      val result  = controller.unauthenticatedAccessibilityForm(
        service = None,
        userAction = Some("test?1234=xyz"),
        referrerUrl = None
      )(request)

      status(result) should be(200)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttributeValue("name", "referrer").first().`val`() shouldBe "n/a"
    }

    "display errors when form isn't filled out at all" in new TestScope {

      val request = generateRequest(desc = "", formName = "", email = "", isJavascript = false, referrer = "/somepage")
      val result  = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      val errors   = document.select(".govuk-error-message").asScala
      errors.length should be(3)

      document.title()                                                                     should be("Error: " + Messages("accessibility.title"))
      errors.exists(_.text().contains(Messages("accessibility.problem.error.required"))) shouldBe true
      errors.exists(_.text().contains(Messages("accessibility.name.error.required")))    shouldBe true
      errors.exists(_.text().contains(Messages("accessibility.email.error.required")))   shouldBe true
    }

    "display error messages when message size exceeds limit" in new TestScope {
      val msg2500 = "x" * 2500

      val request = generateRequest(
        desc = msg2500,
        formName = "firstname",
        email = "firstname@email.gov",
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("accessibility.problem.error.length"))) shouldBe true
    }

    "display error messages when email is invalid" in new TestScope {
      val badEmail = "firstname'email.gov."

      val request = generateRequest(
        desc = "valid form message",
        formName = "firstname",
        email = badEmail,
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("accessibility.email.error.invalid"))) shouldBe true
    }

    "display error messages when email is too long" in new TestScope {
      val request = generateRequest(
        desc = "valid form message",
        formName = "firstname",
        email = tooLongEmail,
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.exists(_.text().contains(Messages("accessibility.email.error.length"))) shouldBe true
    }

    "display error messages when name is too long" in new TestScope {
      val longName = "x" * 256

      val request = generateRequest(
        desc = "valid form message",
        formName = longName,
        email = "valid@email.com",
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.exists(_.text().contains(Messages("accessibility.name.error.length"))) shouldBe true
    }

    "redirect to thankyou page when completed" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          name = any[String],
          email = any[String],
          subject = any[String],
          message = any[String],
          referrer = any[String],
          isJavascript = any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(TicketId(1234)))

      val request = generateRequest(
        desc = "valid form message",
        formName = "valid name",
        email = "valid@email.com",
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result)           should be(303)
      header(LOCATION, result) should be(Some("/contact/accessibility-unauthenticated/thanks"))
    }

    "return error page if the Deskpro ticket creation fails" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          name = any[String],
          email = any[String],
          subject = any[String],
          message = any[String],
          referrer = any[String],
          isJavascript = any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new Exception("failed")))

      val request = generateRequest(
        desc = "valid form message",
        formName = "valid name",
        email = "valid@email.com",
        isJavascript = false,
        referrer = "/somepage"
      )

      val result = controller.submitUnauthenticatedAccessibilityForm(None, None)(request)

      status(result) should be(500)

      val document = Jsoup.parse(contentAsString(result))
      document.text() should include("Sorry, there is a problem with the service")
      document.text() should include("Try again later.")
    }
  }

  "Reporting an accessibility problem when logging in" should {

    "return 200 and a valid html page for a request" in new TestScope {

      val request = FakeRequest().withSession(SessionKeys.authToken -> "authToken")
      val result  =
        controller.accessibilityForm(service = Some("my-service"), userAction = Some("test?1234=xyz"))(request)

      status(result) should be(200)
      val document = Jsoup.parse(contentAsString(result))
      document.title()                                                             should be(Messages("accessibility.title"))
      document
        .body()
        .select("form[id=accessibility-form]")
        .first
        .attr("action")                                                          shouldBe s"/contact/accessibility?service=my-service&userAction=test%3F1234%3Dxyz"
      document.getElementById("accessibility-form")                                should not be null
      document.getElementById("service").`val`()                                   should be("my-service")
      document.getElementsByAttributeValue("name", "userAction").first().`val`() shouldBe "test?1234=xyz"
    }

    "display errors when form isn't filled out at all" in new TestScope {

      val request = generateRequest(desc = "", formName = "", email = "", isJavascript = false, referrer = "/somepage")
      val result  =
        controller.submitAccessibilityForm(service = Some("my-service"), Some("my-action"))(
          request.withSession(SessionKeys.authToken -> "authToken")
        )

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title()    should be("Error: " + Messages("accessibility.title"))
      document
        .body()
        .select("form[id=accessibility-form]")
        .first
        .attr("action") shouldBe s"/contact/accessibility?service=my-service&userAction=my-action"
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(3)

      errors.exists(_.text().contains(Messages("accessibility.problem.error.required"))) shouldBe true
      errors.exists(_.text().contains(Messages("accessibility.name.error.required")))    shouldBe true
      errors.exists(_.text().contains(Messages("accessibility.email.error.required")))   shouldBe true
    }

    "display error messages when message size exceeds limit" in new TestScope {
      val msg2500 = "x" * 2500

      val request = generateRequest(
        desc = msg2500,
        formName = "firstname",
        email = "firstname@email.gov",
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  =
        controller.submitAccessibilityForm(None, None)(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("accessibility.problem.error.length"))) shouldBe true
    }

    "display error messages when email is invalid" in new TestScope {
      val badEmail = "firstname'email.gov."

      val request = generateRequest(
        desc = "valid form message",
        formName = "firstname",
        email = badEmail,
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  =
        controller.submitAccessibilityForm(None, None)(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.length should be(1)

      errors.exists(_.text().contains(Messages("accessibility.email.error.invalid"))) shouldBe true
    }

    "display error messages when email is too long" in new TestScope {
      val request = generateRequest(
        desc = "valid form message",
        formName = "firstname",
        email = tooLongEmail,
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  =
        controller.submitAccessibilityForm(None, None)(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.exists(_.text().contains(Messages("accessibility.email.error.length"))) shouldBe true
    }

    "display error messages when name is too long" in new TestScope {
      val longName = "x" * 256

      val request = generateRequest(
        desc = "valid form message",
        formName = longName,
        email = "valid@email.com",
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  =
        controller.submitAccessibilityForm(None, None)(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(400)

      import collection.JavaConverters._

      val document = Jsoup.parse(contentAsString(result))
      document.title() should be("Error: " + Messages("accessibility.title"))
      val errors = document.select(".govuk-error-message").asScala
      errors.exists(_.text().contains(Messages("accessibility.name.error.length"))) shouldBe true
    }

    "redirect to thankyou page when completed" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          name = any[String],
          email = any[String],
          subject = any[String],
          message = any[String],
          referrer = any[String],
          isJavascript = any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(TicketId(1234)))

      val request = generateRequest(
        desc = "valid form message",
        formName = "valid name",
        email = "valid@email.com",
        isJavascript = false,
        referrer = "/somepage"
      )
      val result  =
        controller.submitAccessibilityForm(None, None)(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(303)
      header(LOCATION, result) should be(Some("/contact/accessibility/thanks"))
    }

    "return error page if the Deskpro ticket creation fails" in new TestScope {
      when(
        hmrcDeskproConnector.createDeskProTicket(
          name = any[String],
          email = any[String],
          subject = any[String],
          message = any[String],
          referrer = any[String],
          isJavascript = any[Boolean],
          any[Request[AnyRef]](),
          any[Option[Enrolments]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new Exception("failed")))

      val request = generateRequest(
        desc = "valid form message",
        formName = "valid name",
        email = "valid@email.com",
        isJavascript = false,
        referrer = "/somepage"
      )

      val result =
        controller.submitAccessibilityForm(None, None)(request.withSession(SessionKeys.authToken -> "authToken"))

      status(result) should be(500)

      val document = Jsoup.parse(contentAsString(result))
      document.text() should include("Sorry, there is a problem with the service")
      document.text() should include("Try again later.")
    }
  }

  class TestScope extends MockitoSugar {

    val hmrcDeskproConnector: HmrcDeskproConnector = mock[HmrcDeskproConnector]

    val authConnector: AuthConnector = new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[A] =
        Future.successful(Json.parse("{ \"allEnrolments\" : []}").as[A](retrieval.reads))
    }

    implicit val cconfig: CFConfig = new CFConfig(app.configuration)

    val messages                                  = app.injector.instanceOf[MessagesApi]
    val playFrontendAccessibilityPage             = app.injector.instanceOf[views.html.AccessibilityProblemPage]
    val playFrontendAccessibilityConfirmationPage =
      app.injector.instanceOf[views.html.AccessibilityProblemConfirmationPage]
    val errorPage                                 = app.injector.instanceOf[views.html.InternalErrorPage]

    val controller = new AccessibilityController(
      hmrcDeskproConnector,
      authConnector,
      Configuration(),
      Stubs.stubMessagesControllerComponents(messagesApi = messages),
      playFrontendAccessibilityPage,
      playFrontendAccessibilityConfirmationPage,
      errorPage
    )(cconfig, ExecutionContext.Implicits.global)

    def generateRequest(desc: String, formName: String, email: String, isJavascript: Boolean, referrer: String) = {
      val fields = Map(
        "problemDescription" -> desc,
        "name"               -> formName,
        "email"              -> email,
        "csrfToken"          -> "token",
        "isJavascript"       -> isJavascript.toString,
        "csrfToken"          -> "a-csrf-token",
        "referrer"           -> referrer,
        "service"            -> "unit-test",
        "userAction"         -> "/test/url/action"
      )

      FakeRequest()
        .withHeaders((REFERER, referrer))
        .withFormUrlEncodedBody(fields.toSeq: _*)
    }
  }

}

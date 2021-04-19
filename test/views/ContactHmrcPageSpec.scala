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

package views

import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import config.AppConfig
import controllers.ContactForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ContactHmrcPage

class ContactHmrcPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/contact-hmrc").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val contactHmrcForm: Form[ContactForm] = Form[ContactForm](
    mapping(
      "contact-name"     -> text.verifying("contact.name.error.required", msg => !msg.isEmpty),
      "contact-email"    -> text.verifying("contact.email.error.required", msg => !msg.isEmpty),
      "contact-comments" -> text.verifying("contact.comments.error.required", msg => !msg.isEmpty),
      "isJavascript"     -> boolean,
      "referrer"         -> text,
      "csrfToken"        -> text,
      "service"          -> optional(text),
      "userAction"       -> optional(text)
    )(ContactForm.apply)(ContactForm.unapply)
  )

  val formValues: ContactForm = ContactForm(
    contactName = "Test Person",
    contactEmail = "test@example.com",
    contactComments = "Testing action",
    isJavascript = false,
    referrer = "referrer",
    csrfToken = "12345",
    service = None,
    userAction = None
  )

  val action: Call = Call(method = "POST", url = "/contact/contact-hmrc/submit")

  "the Contact Hmrc standalone page" should {
    val contactHmrcPage = app.injector.instanceOf[ContactHmrcPage]
    val content         = contactHmrcPage(contactHmrcForm, action)

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "translate the hmrc banner into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = contactHmrcPage(contactHmrcForm, action)

      val banners = welshContent.select(".hmrc-organisation-logo")
      banners            should have size 1
      banners.first.text should be("Cyllid a Thollau EM")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "Help and contact - GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Help and contact"
    }

    "return the introductory content" in {
      contentAsString(content) should include(
        "If you need help using your tax account please fill in this form."
      )
    }

    "translate the help text into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = contactHmrcPage(contactHmrcForm, action)

      val paragraphs = welshContent.select("p.govuk-body")
      paragraphs.first.text should include("Os oes gennych ymholiad penodol ynghylch treth Cysylltu â CThEM.")
    }

    "include the correct form tag" in {
      val forms = content.select("form[id=contact-hmrc-form]")
      forms                             should have size 1
      forms.first.attr("method")        should be("POST")
      forms.first.hasAttr("novalidate") should be(true)
    }

    "include the correct form action attribute" in {
      val content = contactHmrcPage(contactHmrcForm, action)

      val forms = content.select("form[id=contact-hmrc-form]")
      forms                      should have size 1
      forms.first.attr("action") should be("/contact/contact-hmrc/submit")
    }

    "include a CSRF token as a hidden input" in {
      content.select("input[name=csrfToken]") should have size 1
    }

    "include the service hidden input" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fill(
          formValues.copy(service = Some("my-service-name"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=service]")
      input.attr("value") should be("my-service-name")
      input.attr("type")  should be("hidden")
    }

    "include the referrer hidden input" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fill(
          formValues.copy(referrer = "my-referrer-url")
        ),
        action
      )
      val input              = contentWithService.select("input[name=referrer]")
      input.attr("type")  should be("hidden")
      input.attr("value") should be("my-referrer-url")
    }

    "include the userAction hidden input" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fill(
          formValues.copy(userAction = Some("my-user-action"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=userAction]")
      input.attr("value") should be("my-user-action")
      input.attr("type")  should be("hidden")
    }

    "not initially include an error summary" in {
      val errorSummaries = content.select(".govuk-error-summary")
      errorSummaries should have size 0
    }

    "include an error summary if errors occur" in {
      val contentWithErrors = contactHmrcPage(
        contactHmrcForm.fillAndValidate(
          formValues.copy(contactName = "", contactEmail = "", contactComments = "")
        ),
        action
      )
      val errorSummaries    = contentWithErrors.select(".govuk-error-summary")
      errorSummaries                         should have size 1
      errorSummaries.first.select("h2").text should include("There is a problem")
    }

    "include Error: in the title if errors occur" in {
      val contentWithErrors = contactHmrcPage(
        contactHmrcForm.fillAndValidate(
          formValues.copy(contactName = "", contactEmail = "", contactComments = "")
        ),
        action
      )
      asDocument(contentWithErrors).title should be("Error: Help and contact - GOV.UK")
    }

    "include the contact comment" in {
      content.select("textarea[name=contact-comments]") should have size 1
    }

    "include the contact comments label" in {
      val label = content.select("label[for=contact-comments]")
      label              should have size 1
      label.first.text shouldBe "Your comments"
    }

    "not initially include an error message for the contact comments input" in {
      val errors = content.select("#contact-comments-error")
      errors should have size 0
    }

    "include an error message for the problem input when a validation error exists" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fillAndValidate(
          formValues.copy(contactComments = "")
        ),
        action
      )
      val errors             = contentWithService.select("#contact-comments-error")
      errors            should have size 1
      errors.first.text should include("Error: Enter your comments")
    }

    "include the submitted problem input value" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fill(
          formValues.copy(contactComments = "Something went wrong for me")
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=contact-comments]")
      inputs            should have size 1
      inputs.first.text should include("Something went wrong for me")
    }

    "translate the contact comments textarea label into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = contactHmrcPage(contactHmrcForm, action)

      val paragraphs = welshContent.select("label[for=contact-comments]")
      paragraphs.first.text should be("Eich sylwadau")
    }

    "include a name input" in {
      content.select("input[name=contact-name]") should have size 1
    }

    "include a name input with spellcheck turned off" in {
      val inputs = content.select("input[name=contact-name]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a label for the name input" in {
      val label = content.select("label[for=contact-name]")
      label              should have size 1
      label.first.text shouldBe "Full name"
    }

    "not initially include an error message for the name input" in {
      val errors = content.select("#contact-name-error")
      errors should have size 0
    }

    "include an error message for the name input" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fillAndValidate(
          formValues.copy(contactName = "")
        ),
        action
      )
      val errors             = contentWithService.select("#contact-name-error")
      errors            should have size 1
      errors.first.text should include("Enter your full name")
    }

    "include the submitted name input value" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fill(
          formValues.copy(contactName = "AN Other")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=contact-name]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("AN Other")
    }

    "include an email input" in {
      content.select("input[name=contact-email]") should have size 1
    }

    "include an email input with spellcheck turned off" in {
      val inputs = content.select("input[name=contact-email]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a email input with the correct type" in {
      val inputs = content.select("input[name=contact-email]")

      inputs.first.attr("type") should be("email")
    }

    "include a label for the email input" in {
      val label = content.select("label[for=contact-email]")
      label              should have size 1
      label.first.text shouldBe "Email address"
    }

    "not initially include an error message for the email input" in {
      val errors = content.select("#contact-email-error")
      errors should have size 0
    }

    "include an error message for the email input if a validation error exists" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fillAndValidate(
          formValues.copy(contactEmail = "")
        ),
        action
      )
      val errors             = contentWithService.select("#contact-email-error")
      errors            should have size 1
      errors.first.text should include("Enter your email address")
    }

    "include the submitted email input value" in {
      val contentWithService = contactHmrcPage(
        contactHmrcForm.fill(
          formValues.copy(contactEmail = "bloggs@example.com")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=contact-email]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("bloggs@example.com")
    }

    "include a submit button" in {
      val buttons = content.select("button[type=submit]")
      buttons              should have size 1
      buttons.first.text shouldBe "Send"
    }

    "have double click prevention turned on" in {
      val buttons = content.select("button[type=submit]")
      buttons.first.attr("data-prevent-double-click") shouldBe "true"
    }
  }
}

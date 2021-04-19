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

import config.AppConfig
import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import model.AccessibilityForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Call, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.AccessibilityProblemPage
import play.api.test.CSRFTokenHelper._

class AccessibilityProblemPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val accessibilityForm: Form[AccessibilityForm] = Form[AccessibilityForm](
    mapping(
      "problemDescription" -> text.verifying("accessibility.problem.error.required", msg => !msg.isEmpty),
      "name"               -> text.verifying("accessibility.name.error.required", msg => !msg.isEmpty),
      "email"              -> text.verifying("accessibility.email.error.invalid", msg => !msg.isEmpty),
      "isJavascript"       -> boolean,
      "referrer"           -> text,
      "csrfToken"          -> text,
      "service"            -> optional(text),
      "userAction"         -> optional(text)
    )(AccessibilityForm.apply)(AccessibilityForm.unapply)
  )

  val formValues: AccessibilityForm = AccessibilityForm(
    problemDescription = "",
    name = "",
    email = "",
    isJavascript = false,
    referrer = "",
    csrfToken = "",
    service = None,
    userAction = None
  )

  val action: Call = Call(method = "POST", url = "/contact/the-submit-url")

  "the report an accessibility problem page" should {
    val accessibilityProblemPage = app.injector.instanceOf[AccessibilityProblemPage]
    val content                  = accessibilityProblemPage(accessibilityForm, action)

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "translate the hmrc banner into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = accessibilityProblemPage(accessibilityForm, action)

      val banners = welshContent.select(".hmrc-organisation-logo")
      banners            should have size 1
      banners.first.text should be("Cyllid a Thollau EM")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "Report an accessibility problem - GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Report an accessibility problem"
    }

    "return the introductory content" in {
      contentAsString(content) should include(
        "Please only use this form to report any accessibility problems you have found when using this service"
      )
    }

    "translate the help text into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = accessibilityProblemPage(accessibilityForm, action)

      val paragraphs = welshContent.select("p.govuk-body")
      paragraphs.first.text should include("Dylech dim ond defnyddio’r")
    }

    "include the correct form tag" in {
      val forms = content.select("form[id=accessibility-form]")
      forms                             should have size 1
      forms.first.attr("method")        should be("POST")
      forms.first.hasAttr("novalidate") should be(true)
    }

    "include the correct form action attribute" in {
      val content = accessibilityProblemPage(accessibilityForm, action)

      val forms = content.select("form[id=accessibility-form]")
      forms                      should have size 1
      forms.first.attr("action") should be("/contact/the-submit-url")
    }

    "include a CSRF token as a hidden input" in {
      content.select("input[name=csrfToken]") should have size 1
    }

    "include the service hidden input" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fill(
          formValues.copy(service = Some("foo"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=service]")
      input.attr("value") should be("foo")
      input.attr("type")  should be("hidden")
    }

    "include the referrer hidden input" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fill(
          formValues.copy(referrer = "bar")
        ),
        action
      )
      val input              = contentWithService.select("input[name=referrer]")
      input.attr("type")  should be("hidden")
      input.attr("value") should be("bar")
    }

    "include the userAction hidden input" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fill(
          formValues.copy(userAction = Some("baz"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=userAction]")
      input.attr("value") should be("baz")
      input.attr("type")  should be("hidden")
    }

    "not initially include an error summary" in {
      val errorSummaries = content.select(".govuk-error-summary")
      errorSummaries should have size 0
    }

    "include an error summary if errors occur" in {
      val contentWithErrors = accessibilityProblemPage(
        accessibilityForm.fillAndValidate(
          formValues.copy(problemDescription = "", name = "", email = "")
        ),
        action
      )
      val errorSummaries    = contentWithErrors.select(".govuk-error-summary")
      errorSummaries                         should have size 1
      errorSummaries.first.select("h2").text should include("There is a problem")
    }

    "include Error: in the title if errors occur" in {
      val contentWithErrors = accessibilityProblemPage(
        accessibilityForm.fillAndValidate(
          formValues.copy(problemDescription = "", name = "", email = "")
        ),
        action
      )
      asDocument(contentWithErrors).title should be("Error: Report an accessibility problem - GOV.UK")
    }

    "include the problem input" in {
      content.select("textarea[name=problemDescription]") should have size 1
    }

    "include the problem input label" in {
      val label = content.select("label[for=problemDescription]")
      label              should have size 1
      label.first.text shouldBe "Describe the accessibility problem you have found"
    }

    "not initially include an error message for the problem input" in {
      val errors = content.select("#problemDescription-error")
      errors should have size 0
    }

    "include an error message for the problem input when a validation error exists" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fillAndValidate(
          formValues.copy(problemDescription = "")
        ),
        action
      )
      val errors             = contentWithService.select("#problemDescription-error")
      errors            should have size 1
      errors.first.text should include("Enter details of the accessibility problem")
    }

    "include the submitted problem input value" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fill(
          formValues.copy(problemDescription = "I have a problem")
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=problemDescription]")
      inputs            should have size 1
      inputs.first.text should include("I have a problem")
    }

    "translate the textarea label into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = accessibilityProblemPage(accessibilityForm, action)

      val paragraphs = welshContent.select("label[for=problemDescription]")
      paragraphs.first.text should be("Disgrifiwch y broblem hygyrchedd rydych wedi dod o hyd iddi")
    }

    "include a name input" in {
      content.select("input[name=name]") should have size 1
    }

    "include a name input with spellcheck turned off" in {
      val inputs = content.select("input[name=name]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a name input with autocomplete" in {
      val inputs = content.select("input[name=name]")

      inputs.first.attr("autocomplete") should be("name")
    }

    "include a label for the name input" in {
      val label = content.select("label[for=name]")
      label              should have size 1
      label.first.text shouldBe "Full name"
    }

    "not initially include an error message for the name input" in {
      val errors = content.select("#name-error")
      errors should have size 0
    }

    "include an error message for the name input" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fillAndValidate(
          formValues.copy(name = "")
        ),
        action
      )
      val errors             = contentWithService.select("#name-error")
      errors            should have size 1
      errors.first.text should include("Enter your full name")
    }

    "include the submitted name input value" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fill(
          formValues.copy(name = "AN Other")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=name]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("AN Other")
    }

    "include an email input" in {
      content.select("input[name=email]") should have size 1
    }

    "include an email input with spellcheck turned off" in {
      val inputs = content.select("input[name=email]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a email input with autocomplete" in {
      val inputs = content.select("input[name=email]")

      inputs.first.attr("autocomplete") should be("email")
    }

    "include a email input with the correct type" in {
      val inputs = content.select("input[name=email]")

      inputs.first.attr("type") should be("email")
    }

    "include a label for the email input" in {
      val label = content.select("label[for=email]")
      label              should have size 1
      label.first.text shouldBe "Email address"
    }

    "include a hint for the email input" in {
      val label = content.select("#email-hint")
      label            should have size 1
      label.first.text should include("We will only use this to reply to your message.")
    }

    "not initially include an error message for the email input" in {
      val errors = content.select("#email-error")
      errors should have size 0
    }

    "include an error message for the email input if a validation error exists" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fillAndValidate(
          formValues.copy(email = "")
        ),
        action
      )
      val errors             = contentWithService.select("#email-error")
      errors            should have size 1
      errors.first.text should include("Enter an email address in the correct format, like name@example.com")
    }

    "include the submitted email input value" in {
      val contentWithService = accessibilityProblemPage(
        accessibilityForm.fill(
          formValues.copy(email = "bloggs@example.com")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=email]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("bloggs@example.com")
    }

    "include a submit button" in {
      val buttons = content.select("button[type=submit]")
      buttons              should have size 1
      buttons.first.text shouldBe "Report Problem"
    }

    "have double click prevention turned on" in {
      val buttons = content.select("button[type=submit]")
      buttons.first.attr("data-prevent-double-click") shouldBe "true"
    }
  }
}

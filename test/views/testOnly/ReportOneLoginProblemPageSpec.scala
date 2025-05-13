/*
 * Copyright 2025 HM Revenue & Customs
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

package views.testOnly

import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import config.AppConfig
import model.{DateOfBirth, ReportOneLoginProblemForm}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.testOnly.ReportOneLoginProblemPage

class ReportOneLoginProblemPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  given fakeRequest: RequestHeader = FakeRequest("GET", "/problem_reports_nonjs").withCSRFToken
  given Messages                   = getMessages()
  given AppConfig                  = app.injector.instanceOf[AppConfig]

  val oneLoginProblemReportsForm: Form[ReportOneLoginProblemForm] = Form[ReportOneLoginProblemForm](
    mapping(
      "name"               -> text.verifying("problem_report.name.error.required", msg => msg.nonEmpty),
      "nino"               -> text.verifying("problem_report.name.error.required", msg => msg.nonEmpty),
      "sa-utr"             -> optional(text),
      "date-of-birth"      -> mapping(
        "day"   -> text,
        "month" -> text,
        "year"  -> text
      )(DateOfBirth.apply)(d => Some(Tuple.fromProductTyped(d))),
      "email"              -> text.verifying("problem_report.email.error.required", msg => msg.nonEmpty),
      "phone-number"       -> optional(text),
      "address"            -> text.verifying("problem_report.name.error.required", msg => msg.nonEmpty),
      "contact-preference" -> optional(text),
      "complaint"          -> optional(text),
      "csrfToken"          -> text
    )(ReportOneLoginProblemForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  val formValues: ReportOneLoginProblemForm = ReportOneLoginProblemForm(
    name = "Test Person",
    nino = "AA123456D",
    saUtr = Some("1234567890"),
    dateOfBirth = DateOfBirth("1", "1", "1990"),
    email = "test@example.com",
    phoneNumber = Some("020 7123 4567"),
    address = "1 Whitehall, London, SW1A",
    contactPreference = Some("Email"),
    complaint = Some("Testing complaint"),
    csrfToken = ""
  )

  val action: Call = Call(method = "POST", url = "/contact/submit-error-feedback")

  "the Problem Reports standalone page" should {
    val reportProblemPage = app.injector.instanceOf[ReportOneLoginProblemPage]
    val content           = reportProblemPage(oneLoginProblemReportsForm, action)

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "translate the hmrc banner into Welsh if requested" in {
      given Messages   = getWelshMessages()
      val welshContent = reportProblemPage(oneLoginProblemReportsForm, action)

      val banners = welshContent.select(".hmrc-organisation-logo")
      banners            should have size 1
      banners.first.text should be("Cyllid a Thollau EF")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").first().text shouldBe "Your complaint"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Your complaint"
    }

//    "return the introductory content" in {
//      contentAsString(content) should include(
//        "Only use this form to report technical problems."
//      )
//    }

//    "translate the help text into Welsh if requested" in {
//      given Messages   = getWelshMessages()
//      val welshContent = reportProblemPage(oneLoginProblemReportsForm, action)
//
//      val paragraphs = welshContent.select("p.govuk-body")
//      paragraphs.first.text should include("Defnyddio’r ffurflen hon i roi gwybod am broblemau technegol yn unig.")
//    }

//    "include the correct form tag" in {
//      val forms = content.select("form[id=error-feedback-form]")
//      forms                             should have size 1
//      forms.first.attr("method")        should be("POST")
//      forms.first.hasAttr("novalidate") should be(true)
//    }
//
//    "include the correct form action attribute" in {
//      val content = reportProblemPage(oneLoginProblemReportsForm, action)
//
//      val forms = content.select("form[id=error-feedback-form]")
//      forms                      should have size 1
//      forms.first.attr("action") should be("/contact/submit-error-feedback")
//    }

    "include a CSRF token as a hidden input" in {
      val input = content.select("input[name=csrfToken]")
      input              should have size 1
      input.attr("type") should be("hidden")
    }

    "not initially include an error summary" in {
      val errorSummaries = content.select(".govuk-error-summary")
      errorSummaries should have size 0
    }

    "include an error summary if errors occur" in {
      val contentWithErrors = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(name = "", email = "", complaint = None)
        ),
        action
      )
      val errorSummaries    = contentWithErrors.select(".govuk-error-summary")
      errorSummaries                         should have size 1
      errorSummaries.first.select("h2").text should include("There is a problem")
    }

    "include Error: in the title if errors occur" in {
      val contentWithErrors = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(name = "", email = "", complaint = None)
        ),
        action
      )
      asDocument(contentWithErrors).title should be("Error: Your complaint")
    }

    "include a name input" in {
      content.select("input[name=name]") should have size 1
    }

    "include a name input with spellcheck turned off" in {
      val inputs = content.select("input[name=name]")

      inputs.first.attr("spellcheck") should be("false")
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
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(name = "")
        ),
        action
      )
      val errors             = contentWithService.select("#name-error")
      errors            should have size 1
      errors.first.text should include("Enter your full name")
    }

    "include the submitted name input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
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

    "include a email input with the correct type" in {
      val inputs = content.select("input[name=email]")

      inputs.first.attr("type") should be("email")
    }

    "include a label for the email input" in {
      val label = content.select("label[for=email]")
      label              should have size 1
      label.first.text shouldBe "Email address"
    }

    "not initially include an error message for the email input" in {
      val errors = content.select("#email-error")
      errors should have size 0
    }

    "include an error message for the email input if a validation error exists" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(email = "")
        ),
        action
      )
      val errors             = contentWithService.select("#email-error")
      errors            should have size 1
      errors.first.text should include("Enter your email address")
    }

    "include the submitted email input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
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
      buttons.first.text shouldBe "Send"
    }

    "have double click prevention turned on" in {
      val buttons = content.select("button[type=submit]")
      buttons.first.attr("data-prevent-double-click") shouldBe "true"
    }
  }
}

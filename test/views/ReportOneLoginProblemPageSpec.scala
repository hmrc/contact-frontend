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

package views

import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import config.AppConfig
import controllers.testOnly.ReportOneLoginProblemFormBind
import model.{DateOfBirth, ReportOneLoginProblemForm}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ReportOneLoginProblemPage

import java.time.LocalDate

class ReportOneLoginProblemPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  given fakeRequest: RequestHeader = FakeRequest("GET", "/problem_reports_nonjs").withCSRFToken
  given Messages                   = getMessages()
  given AppConfig                  = app.injector.instanceOf[AppConfig]

  val oneLoginProblemReportsForm = ReportOneLoginProblemFormBind.form

  val formValues: ReportOneLoginProblemForm = ReportOneLoginProblemForm(
    name = "Test Person",
    nino = "AA123456D",
    saUtr = Some("1234567890"),
    dateOfBirth = DateOfBirth("1", "1", "1990"),
    email = "test@example.com",
    phoneNumber = Some("020 7123 4567"),
    address = "1 Whitehall, London, SW1A",
    contactPreference = "email",
    complaint = Some("Testing complaint"),
    csrfToken = ""
  )

  val action: Call = Call(method = "POST", url = "/contact/test-only/report-one-login-problem")

  "the OlfG Problem Reports standalone page" should {
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
      content.select("title").first().text shouldBe "One Login for Government complaint – GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "One Login for Government complaint"
    }

    "include the correct form tag" in {
      val forms = content.select("form[id=error-feedback-form]")
      forms                             should have size 1
      forms.first.attr("method")        should be("POST")
      forms.first.hasAttr("novalidate") should be(true)
    }

    "include the correct form action attribute" in {
      val content = reportProblemPage(oneLoginProblemReportsForm, action)

      val forms = content.select("form[id=error-feedback-form]")
      forms                      should have size 1
      forms.first.attr("action") should be("/contact/test-only/report-one-login-problem")
    }

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
      asDocument(contentWithErrors).title should be("Error: One Login for Government complaint – GOV.UK")
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

    "include a nino input" in {
      content.select("input[name=nino]") should have size 1
    }

    "include a nino input with spellcheck turned off" in {
      val inputs = content.select("input[name=nino]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a label for the nino input" in {
      val label = content.select("label[for=nino]")
      label              should have size 1
      label.first.text shouldBe "National Insurance number"
    }

    "include a hint for the nino input" in {
      val hint = content.select("div[id=nino-hint]")
      hint              should have size 1
      hint.first.text shouldBe "It's on your National Insurance card, benefit letter, payslip or P60 - for example, 'QQ 12 34 56 C'"
    }

    "not initially include an error message for the nino input" in {
      val errors = content.select("#nino-error")
      errors should have size 0
    }

    "include an error message for the empty nino input" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(nino = "")
        ),
        action
      )
      val errors             = contentWithService.select("#nino-error")
      errors            should have size 1
      errors.first.text should include("Error: Enter a National Insurance number in the correct format")
    }

    "include an error message for the incorrect nino input" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(nino = "incorrect nino")
        ),
        action
      )
      val errors             = contentWithService.select("#nino-error")
      errors            should have size 1
      errors.first.text should include("Error: Enter a National Insurance number in the correct format")
    }

    "include the submitted nino input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(nino = "AN Other")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=nino]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("AN Other")
    }

    "include a sa-utr input" in {
      content.select("input[name=sa-utr]") should have size 1
    }

    "include a sa-utr input with spellcheck turned off" in {
      val inputs = content.select("input[name=sa-utr]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a label for the sa-utr input" in {
      val label = content.select("label[for=sa-utr]")
      label              should have size 1
      label.first.text shouldBe "Self Assessment UTR"
    }

    "include a hint for the sa-utr input" in {
      val label = content.select("div[id=sa-utr-hint]")
      label              should have size 1
      label.first.text shouldBe "You can find it in your Personal Tax Account, the HMRC app or on tax returns and other documents from HMRC. It might be called 'reference', 'UTR' or 'official use'."
    }

    "not initially include an error message for the sa-utr input" in {
      val errors = content.select("#sa-utr-error")
      errors should have size 0
    }

    "include an error message for the incorrect sa-utr input" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(saUtr = Some("incorrect UTR"))
        ),
        action
      )
      val errors             = contentWithService.select("#sa-utr-error")
      errors            should have size 1
      errors.first.text should include(
        "Error: Enter your Self Assessment Unique Taxpayer Reference in the correct format or leave empty"
      )
    }

    "include the submitted sa-utr input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(saUtr = Some("AN Other"))
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=sa-utr]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("AN Other")
    }

    "include a date of birth input" in {
      content.select("input[name=date-of-birth.day]")   should have size 1
      content.select("input[name=date-of-birth.month]") should have size 1
      content.select("input[name=date-of-birth.year]")  should have size 1
    }

    "include a label for the date of birth input" in {
      val label = content.select("legend")
      label              should have size 2
      label.first.text shouldBe "Date of birth"
    }

    "include a hint for the date of birth input" in {
      val label = content.select("div[id=date-of-birth-hint]")
      label              should have size 1
      label.first.text shouldBe "For example, 27 3 2024"
    }

    "not initially include an error message for the date of birth input" in {
      val errors = content.select("#date-of-birth-error")
      errors should have size 0
    }

    "include an error message for the empty date of birth input" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(dateOfBirth = DateOfBirth("", "", ""))
        ),
        action
      )
      val errors             = contentWithService.select("#date-of-birth-error")
      errors            should have size 1
      errors.first.text should include("Error: Date of birth must include a day")
    }

//    TODO: This is not triggering the error, why not?
//    "include an error message for the incorrect date of birth input" in {
//      val contentWithService = reportProblemPage(
//        oneLoginProblemReportsForm.fillAndValidate(
//          formValues.copy(dateOfBirth = DateOfBirth("32", "13", "1000"))
//        ),
//        action
//      )
//
//      val errors             = contentWithService.select("#date-of-birth-error")
//      errors            should have size 1
//      errors.first.text should include("Error: Date of birth must be a valid date")
//    }

//    TODO: This is not triggering the error, why not?
//    "include an error message for the future date of birth input" in {
//      val futureDateOfBirth = {
//        val futureDate = LocalDate.now().plusDays(10)
//        DateOfBirth(
//          day = futureDate.getDayOfMonth.toString,
//          month = futureDate.getMonthValue.toString,
//          year = futureDate.getYear.toString
//        )
//      }
//
//      val contentWithService = reportProblemPage(
//        oneLoginProblemReportsForm.fillAndValidate(
//          formValues.copy(dateOfBirth = futureDateOfBirth)
//        ),
//        action
//      )
//      val errors             = contentWithService.select("#date-of-birth-error")
//      errors            should have size 1
//      errors.first.text should include("Error: Date of birth must not be in the future")
//    }

    "include the submitted date of birth input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(dateOfBirth = DateOfBirth("10", "11", "1990"))
        ),
        action
      )
      val dayInput           = contentWithService.select("input[name=date-of-birth.day]")
      val monthInput         = contentWithService.select("input[name=date-of-birth.month]")
      val yearInput          = contentWithService.select("input[name=date-of-birth.year]")
      dayInput                       should have size 1
      monthInput                     should have size 1
      yearInput                      should have size 1
      dayInput.first.attr("value")   should include("10")
      monthInput.first.attr("value") should include("11")
      yearInput.first.attr("value")  should include("1990")
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

    "include a phone number input" in {
      content.select("input[name=phone-number]") should have size 1
    }

    "include a phone number input with spellcheck turned off" in {
      val inputs = content.select("input[name=phone-number]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a label for the phone number input" in {
      val label = content.select("label[for=phone-number]")
      label              should have size 1
      label.first.text shouldBe "Phone number (optional)"
    }

    "not initially include an error message for the phone number input" in {
      val errors = content.select("#phone-number-error")
      errors should have size 0
    }

    "include the submitted phone number input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(phoneNumber = Some("01234123123"))
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=phone-number]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("01234123123")
    }

    "include a address input" in {
      content.select("textarea[name=address]") should have size 1
    }

    "include a label for the address input" in {
      val label = content.select("label[for=address]")
      label              should have size 1
      label.first.text shouldBe "What is your address?"
    }

    "not initially include an error message for the address input" in {
      val errors = content.select("#address-error")
      errors should have size 0
    }

    "include an error message for the empty address input" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fillAndValidate(
          formValues.copy(address = "")
        ),
        action
      )
      val errors             = contentWithService.select("#address-error")
      errors            should have size 1
      errors.first.text should include("Error: Enter your full address")
    }

    "include the submitted address input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(address = "address")
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=address]")
      inputs              should have size 1
      inputs.first.text() should include("address")
    }

    "include a contact preference input" in {
      content.select("fieldset[aria-describedby=contact-preference-hint]") should have size 1
    }

    "include a label for the contact preference input" in {
      val label = content.select("legend")

      label             should have size 2
      label.last.text shouldBe "How would you prefer to be contacted?"
    }

    "include a hint for contact preference input" in {
      val hint = content.select("div[id=contact-preference-hint]")
      hint              should have size 1
      hint.first.text shouldBe "Select one option"
    }

    // TODO: Commented until implemented
//    "have email as default value in contact preference" in {
//      val inputs = content.select("input[name=contact-preference]")
//      inputs                 should have size 3
//      inputs.get(0).toString should include("checked")
//      inputs.get(1).toString shouldNot include("checked")
//      inputs.get(2).toString shouldNot include("checked")
//    }

    "not initially include an error message for the contact preference input" in {
      val errors = content.select("#contact-preference-error")
      errors should have size 0
    }

    // TODO: Commented until implemented
//    "include error for incorrect contact preference" in {
//      val contentWithService = reportProblemPage(
//        oneLoginProblemReportsForm.fill(
//          formValues.copy(contactPreference = Some("incorrect"))
//        ),
//        action
//      )
//      val inputs             = contentWithService.select("input[name=contact-preference]")
//      val errors             = content.select("#contact-preference-error")
//      inputs            should have size 3
//      inputs.get(0).toString shouldNot include("checked")
//      inputs.get(1).toString shouldNot include("checked")
//      inputs.get(2).toString shouldNot include("checked")
//      errors            should have size 1
//      errors.first.text should include("Incorrect contact preference")
//    }

    "include the submitted contact preference input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(contactPreference = "email")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=contact-preference]")
      inputs                 should have size 3
      inputs.get(0).toString should include("checked")
      inputs.get(1).toString shouldNot include("checked")
      inputs.get(2).toString shouldNot include("checked")
    }

    "include a complaint input" in {
      content.select("textarea[name=complaint]") should have size 1
    }

    "include a label for the complaint input" in {
      val label = content.select("label[for=complaint]")
      label              should have size 1
      label.first.text shouldBe "Complaint"
    }

    "include a hint for the complaint input" in {
      val label = content.select("div[id=complaint-hint]")
      label              should have size 1
      label.first.text shouldBe "Do not include personal or financial information"
    }

    "not initially include an error message for the complaint input" in {
      val errors = content.select("#complaint-error")
      errors should have size 0
    }

    "include the submitted complaint input value" in {
      val contentWithService = reportProblemPage(
        oneLoginProblemReportsForm.fill(
          formValues.copy(complaint = Some("complaint text"))
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=complaint]")
      inputs              should have size 1
      inputs.first.text() should include("complaint text")
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

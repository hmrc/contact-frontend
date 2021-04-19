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
import model.ProblemReport
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
import views.html.{AccessibilityProblemPage, ProblemReportsNonjsPage}
import play.api.test.CSRFTokenHelper._

class ProblemReportsNonjsPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/problem_reports_nonjs").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val problemReportsForm: Form[ProblemReport] = Form[ProblemReport](
    mapping(
      "report-name"   -> text.verifying("problem_report.name.error.required", msg => !msg.isEmpty),
      "report-email"  -> text.verifying("problem_report.email.error.required", msg => !msg.isEmpty),
      "report-action" -> text.verifying("problem_report.action.error.required", msg => !msg.isEmpty),
      "report-error"  -> text.verifying("problem_report.error.error.required", msg => !msg.isEmpty),
      "isJavascript"  -> boolean,
      "service"       -> optional(text),
      "referrer"      -> optional(text),
      "userAction"    -> optional(text)
    )(ProblemReport.apply)(ProblemReport.unapply)
  )

  val formValues: ProblemReport = ProblemReport(
    reportName = "Test Person",
    reportEmail = "test@example.com",
    reportAction = "Testing action",
    reportError = "Testing error",
    isJavascript = false,
    referrer = None,
    service = None,
    userAction = None
  )

  val action: Call = Call(method = "POST", url = "/contact/submit-error-feedback")

  "the Problem Reports standalone page" should {
    val problemReportsNonjsPage = app.injector.instanceOf[ProblemReportsNonjsPage]
    val content                 = problemReportsNonjsPage(problemReportsForm, action)

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "translate the hmrc banner into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = problemReportsNonjsPage(problemReportsForm, action)

      val banners = welshContent.select(".hmrc-organisation-logo")
      banners            should have size 1
      banners.first.text should be("Cyllid a Thollau EM")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "Get help with a technical problem - GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Get help with a technical problem"
    }

    "return the introductory content" in {
      contentAsString(content) should include(
        "Only use this form to report technical problems."
      )
    }

    "translate the help text into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = problemReportsNonjsPage(problemReportsForm, action)

      val paragraphs = welshContent.select("p.govuk-body")
      paragraphs.first.text should include("Defnyddio’r ffurflen hon i roi gwybod am broblemau technegol yn unig.")
    }

    "include the correct form tag" in {
      val forms = content.select("form[id=error-feedback-form]")
      forms                             should have size 1
      forms.first.attr("method")        should be("POST")
      forms.first.hasAttr("novalidate") should be(true)
    }

    "include the correct form action attribute" in {
      val content = problemReportsNonjsPage(problemReportsForm, action)

      val forms = content.select("form[id=error-feedback-form]")
      forms                      should have size 1
      forms.first.attr("action") should be("/contact/submit-error-feedback")
    }

    "include a CSRF token as a hidden input" in {
      content.select("input[name=csrfToken]") should have size 1
    }

    "include the service hidden input" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
          formValues.copy(service = Some("my-service-name"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=service]")
      input.attr("value") should be("my-service-name")
      input.attr("type")  should be("hidden")
    }

    "include the referrer hidden input" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
          formValues.copy(referrer = Some("my-referrer-url"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=referrer]")
      input.attr("type")  should be("hidden")
      input.attr("value") should be("my-referrer-url")
    }

    "include the userAction hidden input" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
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
      val contentWithErrors = problemReportsNonjsPage(
        problemReportsForm.fillAndValidate(
          formValues.copy(reportName = "", reportEmail = "", reportAction = "", reportError = "")
        ),
        action
      )
      val errorSummaries    = contentWithErrors.select(".govuk-error-summary")
      errorSummaries                         should have size 1
      errorSummaries.first.select("h2").text should include("There is a problem")
    }

    "include Error: in the title if errors occur" in {
      val contentWithErrors = problemReportsNonjsPage(
        problemReportsForm.fillAndValidate(
          formValues.copy(reportName = "", reportEmail = "", reportAction = "", reportError = "")
        ),
        action
      )
      asDocument(contentWithErrors).title should be("Error: Get help with a technical problem - GOV.UK")
    }

    "include the report action" in {
      content.select("textarea[name=report-action]") should have size 1
    }

    "include the report action label" in {
      val label = content.select("label[for=report-action]")
      label              should have size 1
      label.first.text shouldBe "What were you doing?"
    }

    "not initially include an error message for the report action input" in {
      val errors = content.select("#report-action-error")
      errors should have size 0
    }

    "include an error message for the problem input when a validation error exists" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fillAndValidate(
          formValues.copy(reportAction = "")
        ),
        action
      )
      val errors             = contentWithService.select("#report-action-error")
      errors            should have size 1
      errors.first.text should include("Enter details of what you were doing")
    }

    "include the submitted problem input value" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
          formValues.copy(reportAction = "Something went wrong for me")
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=report-action]")
      inputs            should have size 1
      inputs.first.text should include("Something went wrong for me")
    }

    "translate the report action textarea label into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = problemReportsNonjsPage(problemReportsForm, action)

      val paragraphs = welshContent.select("label[for=report-action]")
      paragraphs.first.text should be("Beth oeddech yn ei wneud?")
    }

    "include the report an error label" in {
      val label = content.select("label[for=report-error]")
      label              should have size 1
      label.first.text shouldBe "What do you need help with?"
    }

    "not initially include an error message for the report an error input" in {
      val errors = content.select("#report-error-error")
      errors should have size 0
    }

    "include an error message for the report an error input when a validation error exists" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fillAndValidate(
          formValues.copy(reportError = "")
        ),
        action
      )
      val errors             = contentWithService.select("#report-error-error")
      errors            should have size 1
      errors.first.text should include("Enter details of what went wrong")
    }

    "include the submitted report an error input value" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
          formValues.copy(reportError = "This was the error I saw")
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=report-error]")
      inputs            should have size 1
      inputs.first.text should include("This was the error I saw")
    }

    "translate the report an error textarea label into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = problemReportsNonjsPage(problemReportsForm, action)

      val paragraphs = welshContent.select("label[for=report-error]")
      paragraphs.first.text should be("Gyda beth ydych angen help?")
    }

    "include a name input" in {
      content.select("input[name=report-name]") should have size 1
    }

    "include a name input with spellcheck turned off" in {
      val inputs = content.select("input[name=report-name]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a label for the name input" in {
      val label = content.select("label[for=report-name]")
      label              should have size 1
      label.first.text shouldBe "Full name"
    }

    "not initially include an error message for the name input" in {
      val errors = content.select("#report-name-error")
      errors should have size 0
    }

    "include an error message for the name input" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fillAndValidate(
          formValues.copy(reportName = "")
        ),
        action
      )
      val errors             = contentWithService.select("#report-name-error")
      errors            should have size 1
      errors.first.text should include("Enter your full name")
    }

    "include the submitted name input value" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
          formValues.copy(reportName = "AN Other")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=report-name]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("AN Other")
    }

    "include an email input" in {
      content.select("input[name=report-email]") should have size 1
    }

    "include an email input with spellcheck turned off" in {
      val inputs = content.select("input[name=report-email]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a email input with the correct type" in {
      val inputs = content.select("input[name=report-email]")

      inputs.first.attr("type") should be("email")
    }

    "include a label for the email input" in {
      val label = content.select("label[for=report-email]")
      label              should have size 1
      label.first.text shouldBe "Email address"
    }

    "not initially include an error message for the email input" in {
      val errors = content.select("#report-email-error")
      errors should have size 0
    }

    "include an error message for the email input if a validation error exists" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fillAndValidate(
          formValues.copy(reportEmail = "")
        ),
        action
      )
      val errors             = contentWithService.select("#report-email-error")
      errors            should have size 1
      errors.first.text should include("Enter your email address")
    }

    "include the submitted email input value" in {
      val contentWithService = problemReportsNonjsPage(
        problemReportsForm.fill(
          formValues.copy(reportEmail = "bloggs@example.com")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=report-email]")
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

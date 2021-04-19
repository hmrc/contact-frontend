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
import model.FeedbackForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.FeedbackPage

class FeedbackPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val form: Form[FeedbackForm] = Form[FeedbackForm](
    mapping(
      "feedback-rating"   -> optional(text)
        .verifying("feedback.rating.error.required", rating => rating.isDefined && rating.get.nonEmpty),
      "feedback-name"     -> text
        .verifying("feedback.name.error.required", name => name.nonEmpty),
      "feedback-email"    -> text
        .verifying("feedback.email.error.invalid", email => email.nonEmpty),
      "feedback-comments" -> text
        .verifying("feedback.comments.error.required", comment => comment.nonEmpty),
      "isJavascript"      -> boolean,
      "referrer"          -> text,
      "csrfToken"         -> text,
      "service"           -> optional(text),
      "backUrl"           -> optional(text),
      "canOmitComments"   -> boolean
    )(FeedbackForm.apply)(FeedbackForm.unapply)
  )

  val formValues: FeedbackForm = FeedbackForm(
    experienceRating = Some(""),
    name = "",
    email = "",
    comments = "",
    javascriptEnabled = false,
    referrer = "n/a",
    csrfToken = "",
    service = Some("unknown"),
    backUrl = None,
    canOmitComments = false
  )

  val action: Call = Call(method = "POST", url = "/contact/the-submit-url")

  "the feedback page" should {
    val feedbackPage = app.injector.instanceOf[FeedbackPage]
    val content      = feedbackPage(form, action)

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "translate the hmrc banner into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = feedbackPage(form, action)

      val banners = welshContent.select(".hmrc-organisation-logo")
      banners            should have size 1
      banners.first.text should be("Cyllid a Thollau EM")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "Send your feedback - GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Send your feedback"
    }

    "return the introductory content" in {
      contentAsString(content) should include(
        "We use your feedback to make our services better."
      )
    }

    "translate the help text into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = feedbackPage(form, action)

      val paragraphs = welshContent.select("p.govuk-body")
      paragraphs.first.text should include("Rydym yn defnyddio’ch adborth i wella ein gwasanaethau.")
    }

    "include the correct form tag" in {
      val forms = content.select("form[id=feedback-form]")
      forms                             should have size 1
      forms.first.attr("method")        should be("POST")
      forms.first.hasAttr("novalidate") should be(true)
    }

    "include the correct form action attribute" in {
      val content = feedbackPage(form, action)

      val forms = content.select("form[id=feedback-form]")
      forms                      should have size 1
      forms.first.attr("action") should be("/contact/the-submit-url")
    }

    "include a CSRF token as a hidden input" in {
      content.select("input[name=csrfToken]") should have size 1
    }

    "include the service hidden input" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(service = Some("foo"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=service]")
      input.attr("value") should be("foo")
      input.attr("type")  should be("hidden")
    }

    "include the canOmitComments hidden input" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(canOmitComments = true)
        ),
        action
      )
      val input              = contentWithService.select("input[name=canOmitComments]")
      input.attr("value") should be("true")
      input.attr("type")  should be("hidden")
    }

    "not include the backUrl hidden input if not specified" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(backUrl = None)
        ),
        action
      )
      contentWithService.select("input[name=backUrl]") should have size 0
    }

    "include the backUrl hidden input" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(backUrl = Some("/bar"))
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=backUrl]")
      inputs                     should have size 1
      inputs.first.attr("value") should be("/bar")
      inputs.first.attr("type")  should be("hidden")
    }

    "include the referrer hidden input" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(referrer = "bar")
        ),
        action
      )
      val input              = contentWithService.select("input[name=referrer]")
      input.attr("type")  should be("hidden")
      input.attr("value") should be("bar")
    }

    "not initially include an error summary" in {
      val errorSummaries = content.select(".govuk-error-summary")
      errorSummaries should have size 0
    }

    "include an error summary if errors occur" in {
      val contentWithErrors = feedbackPage(
        form.fillAndValidate(
          formValues.copy(comments = "", name = "", email = "")
        ),
        action
      )
      val errorSummaries    = contentWithErrors.select(".govuk-error-summary")
      errorSummaries                         should have size 1
      errorSummaries.first.select("h2").text should include("There is a problem")
    }

    "include Error: in the title if errors occur" in {
      val contentWithErrors = feedbackPage(
        form.fillAndValidate(
          formValues.copy(comments = "", name = "", email = "")
        ),
        action
      )
      asDocument(contentWithErrors).title should be("Error: Send your feedback - GOV.UK")
    }

    "include a legend for the feedback rating radios" in {
      val legends = content.select(".govuk-fieldset__legend")

      legends should have size 1

      legends.first.text should be("What do you think of this online service?")
    }

    "include a 'Very good' radio" in {
      val labels = content.select("input[id=feedback-rating]")

      labels should have size 1

      labels.first.attr("name")  should be("feedback-rating")
      labels.first.attr("value") should be("5")
    }

    "include a 'Very good' label" in {
      val label = content.select("label[for=feedback-rating]")
      label              should have size 1
      label.first.text shouldBe "Very good"
    }

    "include a 'Very bad' radio" in {
      val labels = content.select("input[id=feedback-rating-5]")

      labels should have size 1

      labels.first.attr("name")  should be("feedback-rating")
      labels.first.attr("value") should be("1")
    }

    "include a 'Very bad' label" in {
      val label = content.select("label[for=feedback-rating-5]")
      label              should have size 1
      label.first.text shouldBe "Very bad"
    }

    "not initially include an error message for the feedback rating" in {
      val errors = content.select("#feedback-rating-error")
      errors should have size 0
    }

    "include an error message for the feedback rating" in {
      val contentWithService = feedbackPage(
        form.fillAndValidate(
          formValues.copy(experienceRating = Some(""))
        ),
        action
      )
      val errors             = contentWithService.select("#feedback-rating-error")
      errors            should have size 1
      errors.first.text should include("Tell us what you think of the service")
    }

    "include the submitted feedback rating" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(experienceRating = Some("4"))
        ),
        action
      )
      val good               = contentWithService.select("input[id=feedback-rating-2]").first
      good.hasAttr("checked") should be(true)
    }

    "include a name input" in {
      content.select("input[name=feedback-name]") should have size 1
    }

    "include a name input with spellcheck turned off" in {
      val inputs = content.select("input[name=feedback-name]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a name input with autocomplete" in {
      val inputs = content.select("input[name=feedback-name]")

      inputs.first.attr("autocomplete") should be("name")
    }

    "include a label for the name input" in {
      val label = content.select("label[for=feedback-name]")
      label              should have size 1
      label.first.text shouldBe "Full name"
    }

    "not initially include an error message for the name input" in {
      val errors = content.select("#feedback-name-error")
      errors should have size 0
    }

    "include an error message for the name input" in {
      val contentWithService = feedbackPage(
        form.fillAndValidate(
          formValues.copy(name = "")
        ),
        action
      )
      val errors             = contentWithService.select("#feedback-name-error")
      errors            should have size 1
      errors.first.text should include("Enter your full name")
    }

    "include the submitted name input value" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(name = "AN Other")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=feedback-name]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("AN Other")
    }

    "include an email input" in {
      content.select("input[name=feedback-email]") should have size 1
    }

    "include an email input with spellcheck turned off" in {
      val inputs = content.select("input[name=feedback-email]")

      inputs.first.attr("spellcheck") should be("false")
    }

    "include a email input with autocomplete" in {
      val inputs = content.select("input[name=feedback-email]")

      inputs.first.attr("autocomplete") should be("email")
    }

    "include a email input with the correct type" in {
      val inputs = content.select("input[name=feedback-email]")

      inputs.first.attr("type") should be("email")
    }

    "include a label for the email input" in {
      val label = content.select("label[for=feedback-email]")
      label              should have size 1
      label.first.text shouldBe "Email address"
    }

    "not initially include an error message for the email input" in {
      val errors = content.select("#email-error")
      errors should have size 0
    }

    "include an error message for the email input if a validation error exists" in {
      val contentWithService = feedbackPage(
        form.fillAndValidate(
          formValues.copy(email = "")
        ),
        action
      )
      val errors             = contentWithService.select("#feedback-email-error")
      errors            should have size 1
      errors.first.text should include("Enter an email address in the correct format, like name@example.com")
    }

    "include the submitted email input value" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(email = "bloggs@example.com")
        ),
        action
      )
      val inputs             = contentWithService.select("input[name=feedback-email]")
      inputs                     should have size 1
      inputs.first.attr("value") should include("bloggs@example.com")
    }

    "include the comments input" in {
      content.select("textarea[name=feedback-comments]") should have size 1
    }

    "include the comments input label" in {
      val label = content.select("label[for=feedback-comments]")
      label              should have size 1
      label.first.text shouldBe "Comments"
    }

    "include a hint for the comments input" in {
      val label = content.select("#feedback-comments-hint")
      label            should have size 1
      label.first.text should include(
        "Do not include any personal or financial information. For example, your National Insurance or credit card numbers."
      )
    }

    "not initially include an error message for the comments input" in {
      val errors = content.select("#feedback-comments-error")
      errors should have size 0
    }

    "include an error message for the comments input when a validation error exists" in {
      val contentWithService = feedbackPage(
        form.fillAndValidate(
          formValues.copy(comments = "")
        ),
        action
      )
      val errors             = contentWithService.select("#feedback-comments-error")
      errors            should have size 1
      errors.first.text should include("Enter your comments")
    }

    "include the submitted comments input value" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(comments = "I have a problem")
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=feedback-comments]")
      inputs            should have size 1
      inputs.first.text should include("I have a problem")
    }

    "translate the textarea label into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = feedbackPage(form, action)

      val paragraphs = welshContent.select("label[for=feedback-comments]")
      paragraphs.first.text should be("Sylwadau")
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

    "not include a link to the problem reports nonjs form if not specified" in {
      content.body should not include "Is this page not working properly?"
    }

    "include a link to the problem reports nonjs form with service filled from the form" in {
      val contentWithService = feedbackPage(
        form.fill(
          formValues.copy(service = Some("foo"))
        ),
        action
      )

      val links = contentWithService.select("a[href=/contact/problem_reports_nonjs?newTab=true&service=foo]")
      links              should have size 1
      links.first.text shouldBe "Is this page not working properly? (opens in new tab)"
    }
  }
}

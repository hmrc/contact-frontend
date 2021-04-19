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
import model.SurveyForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import views.html.SurveyPage

class SurveyPageSpec extends AnyWordSpec with Matchers with ApplicationSupport with MessagesSupport with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val form: Form[SurveyForm] = Form[SurveyForm](
    mapping(
      "helpful"    -> optional(number(min = 1, max = 5, strict = false))
        .verifying("survey.helpful.error.required", helpful => helpful.isDefined),
      "speed"      -> optional(number(min = 1, max = 5, strict = false))
        .verifying("survey.speed.error.required", speed => speed.isDefined),
      "improve"    -> optional(text)
        .verifying("survey.improve.error.length", improve => improve.getOrElse("").length <= 10),
      "ticket-id"  -> optional(text),
      "service-id" -> optional(text)
    )(SurveyForm.apply)(SurveyForm.unapply)
  )

  val formValues: SurveyForm = SurveyForm(
    helpful = None,
    speed = None,
    improve = None,
    ticketId = Some("ticket-id"),
    serviceId = Some("service-id")
  )

  val action: Call = Call(method = "POST", url = "/contact/the-submit-url")

  "The survey page" should {
    val surveyPage = app.injector.instanceOf[SurveyPage]
    val content    = surveyPage(form, action)

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "translate the hmrc banner into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = surveyPage(form, action)

      val banners = welshContent.select(".hmrc-organisation-logo")
      banners            should have size 1
      banners.first.text should be("Cyllid a Thollau EM")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "Survey - GOV.UK"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Survey"
    }

    "include the correct form tag" in {
      val forms = content.select("form[action=/contact/the-submit-url]")
      forms                             should have size 1
      forms.first.attr("method")        should be("POST")
      forms.first.hasAttr("novalidate") should be(true)
    }

    "include a CSRF token as a hidden input" in {
      content.select("input[name=csrfToken]") should have size 1
    }

    "include the service hidden input" in {
      val contentWithService = surveyPage(
        form.fill(
          formValues.copy(serviceId = Some("foo"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=service-id]")
      input.attr("value") should be("foo")
      input.attr("type")  should be("hidden")
    }

    "include the ticket hidden input" in {
      val contentWithService = surveyPage(
        form.fill(
          formValues.copy(ticketId = Some("bar"))
        ),
        action
      )
      val input              = contentWithService.select("input[name=ticket-id]")
      input.attr("value") should be("bar")
      input.attr("type")  should be("hidden")
    }

    "not initially include an error summary" in {
      val errorSummaries = content.select(".govuk-error-summary")
      errorSummaries should have size 0
    }

    "include an error summary if errors occur" in {
      val contentWithErrors = surveyPage(
        form.fillAndValidate(
          formValues.copy(helpful = None)
        ),
        action
      )
      val errorSummaries    = contentWithErrors.select(".govuk-error-summary")
      errorSummaries                         should have size 1
      errorSummaries.first.select("h2").text should include("There is a problem")
    }

    "include Error: in the title if errors occur" in {
      val contentWithErrors = surveyPage(
        form.fillAndValidate(
          formValues.copy(helpful = None)
        ),
        action
      )
      asDocument(contentWithErrors).title should be("Error: Survey - GOV.UK")
    }

    "include a legend for the helpful radios" in {
      val legends = content.select(".govuk-fieldset__legend")

      legends should have size 2

      legends.get(0).text should be("How satisfied are you with the answer we gave you?")
    }

    "include a 'Very satisfied' helpful radio" in {
      val labels = content.select("input[id=helpful]")

      labels should have size 1

      labels.first.attr("name")  should be("helpful")
      labels.first.attr("value") should be("5")
    }

    "include a 'Very satisfied' helpful label" in {
      val label = content.select("label[for=helpful]")
      label              should have size 1
      label.first.text shouldBe "Very satisfied"
    }

    "include a 'Very dissatisfied' helpful radio" in {
      val labels = content.select("input[id=helpful-5]")

      labels should have size 1

      labels.first.attr("name")  should be("helpful")
      labels.first.attr("value") should be("1")
    }

    "include a 'Very dissatisfied' helpful label" in {
      val label = content.select("label[for=helpful-5]")
      label              should have size 1
      label.first.text shouldBe "Very dissatisfied"
    }

    "not initially include an error message for the helpful rating" in {
      val errors = content.select("#helpful-error")
      errors should have size 0
    }

    "include an error message for the helpful rating" in {
      val contentWithService = surveyPage(
        form.fillAndValidate(
          formValues.copy(helpful = None)
        ),
        action
      )
      val errors             = contentWithService.select("#helpful-error")
      errors            should have size 1
      errors.first.text should include("Tell us how satisfied you are with the answer we gave you")
    }

    "include the submitted helpful rating" in {
      val contentWithService = surveyPage(
        form.fill(
          formValues.copy(helpful = Some(4))
        ),
        action
      )
      val good               = contentWithService.select("input[id=helpful-2]").first
      good.hasAttr("checked") should be(true)
    }

    "include a legend for the speed radios" in {
      val legends = content.select(".govuk-fieldset__legend")

      legends should have size 2

      legends.get(1).text should be("How satisfied are you with the speed of our reply?")
    }

    "include a 'Very satisfied' speed radio" in {
      val labels = content.select("input[id=speed]")

      labels should have size 1

      labels.first.attr("name")  should be("speed")
      labels.first.attr("value") should be("5")
    }

    "include a 'Very satisfied' speed label" in {
      val label = content.select("label[for=speed]")
      label              should have size 1
      label.first.text shouldBe "Very satisfied"
    }

    "include a 'Very dissatisfied' speed radio" in {
      val labels = content.select("input[id=speed-5]")

      labels should have size 1

      labels.first.attr("name")  should be("speed")
      labels.first.attr("value") should be("1")
    }

    "include a 'Very dissatisfied' speed label" in {
      val label = content.select("label[for=speed-5]")
      label              should have size 1
      label.first.text shouldBe "Very dissatisfied"
    }

    "not initially include an error message for the speed rating" in {
      val errors = content.select("#speed-error")
      errors should have size 0
    }

    "include an error message for the speed rating" in {
      val contentWithService = surveyPage(
        form.fillAndValidate(
          formValues.copy(speed = None)
        ),
        action
      )
      val errors             = contentWithService.select("#speed-error")
      errors            should have size 1
      errors.first.text should include("Tell us how satisfied you are with the speed of our reply")
    }

    "include the submitted speed rating" in {
      val contentWithService = surveyPage(
        form.fill(
          formValues.copy(speed = Some(4))
        ),
        action
      )
      val good               = contentWithService.select("input[id=speed-2]").first
      good.hasAttr("checked") should be(true)
    }

    "include the improve input" in {
      content.select("textarea[name=improve]") should have size 1
    }

    "include the comments input label" in {
      val label = content.select("label[for=improve]")
      label              should have size 1
      label.first.text shouldBe "Tell us how we can improve the support we give you (optional)."
    }

    "not initially include an error message for the comments input" in {
      val errors = content.select("#improve-error")
      errors should have size 0
    }

    "include an error message for the comments input when a validation error exists" in {
      val contentWithService = surveyPage(
        form.fillAndValidate(
          formValues.copy(improve = Some("AAAAAAAAAAAAAAAAAAAAAAA"))
        ),
        action
      )
      val errors             = contentWithService.select("#improve-error")
      errors            should have size 1
      errors.first.text should include("Improvement suggestions must be 2500 characters or fewer")
    }

    "include the submitted comments input value" in {
      val contentWithService = surveyPage(
        form.fill(
          formValues.copy(improve = Some("I have a problem"))
        ),
        action
      )
      val inputs             = contentWithService.select("textarea[name=improve]")
      inputs            should have size 1
      inputs.first.text should include("I have a problem")
    }

    "translate the textarea label into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = surveyPage(form, action)

      val paragraphs = welshContent.select("label[for=improve]")
      paragraphs.first.text should be("Rhowch wybod i ni sut y gallwn wella’r cymorth a roddwn i chi.")
    }

    "include a submit button" in {
      val buttons = content.select("button[type=submit]")
      buttons              should have size 1
      buttons.first.text shouldBe "Submit"
    }

    "have double click prevention turned on" in {
      val buttons = content.select("button[type=submit]")
      buttons.first.attr("data-prevent-double-click") shouldBe "true"
    }
  }
}

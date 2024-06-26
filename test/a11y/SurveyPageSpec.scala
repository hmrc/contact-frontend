/*
 * Copyright 2023 HM Revenue & Customs
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

package a11y

import _root_.helpers.{ApplicationSupport, MessagesSupport}
import config.AppConfig
import model.SurveyForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.SurveyPage

class SurveyPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with AccessibilityMatchers {

  given fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken
  given Messages                   = getMessages()
  given AppConfig                  = app.injector.instanceOf[AppConfig]

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
    )(SurveyForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  val action: Call = Call(method = "POST", url = "/contact/the-submit-url")

  "The survey page" should {
    val surveyPage = app.injector.instanceOf[SurveyPage]
    val content    = surveyPage(form, action)

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

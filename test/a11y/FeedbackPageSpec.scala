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
import model.FeedbackForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.FeedbackPage

class FeedbackPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
      with AccessibilityMatchers {

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
    )(FeedbackForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  val action: Call = Call(method = "POST", url = "/contact/the-submit-url")

  "the feedback page" should {
    val feedbackPage = app.injector.instanceOf[FeedbackPage]
    val content      = feedbackPage(form, action)

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

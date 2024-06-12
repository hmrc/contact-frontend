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
import model.ReportProblemForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.data.Forms.*
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper.*
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.ReportProblemPage

class ReportProblemPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with AccessibilityMatchers {

  given fakeRequest: RequestHeader = FakeRequest("GET", "/problem_reports_nonjs").withCSRFToken
  given Messages                   = getMessages()
  given AppConfig                  = app.injector.instanceOf[AppConfig]

  val problemReportsForm: Form[ReportProblemForm] = Form[ReportProblemForm](
    mapping(
      "report-name"   -> text.verifying("problem_report.name.error.required", msg => msg.nonEmpty),
      "report-email"  -> text.verifying("problem_report.email.error.required", msg => msg.nonEmpty),
      "report-action" -> text.verifying("problem_report.action.error.required", msg => msg.nonEmpty),
      "report-error"  -> text.verifying("problem_report.error.error.required", msg => msg.nonEmpty),
      "isJavascript"  -> boolean,
      "service"       -> optional(text),
      "referrer"      -> optional(text),
      "csrfToken"     -> text,
      "userAction"    -> optional(text)
    )(ReportProblemForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  val action: Call = Call(method = "POST", url = "/contact/submit-error-feedback")

  "the Problem Reports standalone page" should {
    val reportProblemPage = app.injector.instanceOf[ReportProblemPage]
    val content           = reportProblemPage(problemReportsForm, action)

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

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

package acceptance.pages

import acceptance.support.Helpers

object ReportProblemPage extends BasePage with Helpers {

  val url: String            = wrapUrl("/problem_reports_nonjs?service=pay")
  val pageTitle: String      = "Get help with a technical problem – GOV.UK"
  val welshPageTitle: String = "Cael help gyda phroblem dechnegol – GOV.UK"

  def nameField: TextField   = textField("report-name")
  def emailField: EmailField = emailField("report-email")
  def actionField: TextArea  = textArea("report-action")
  def errorField: TextArea   = textArea("report-error")

  def completeReportForm(
    name: String = validName,
    email: String = validEmail,
    actionLength: Int = 25,
    errorLength: Int = 25
  ) = {
    nameField.value = name
    emailField.value = email
    actionField.value = generateRandomString(actionLength)
    errorField.value = generateRandomString(errorLength)
  }

}

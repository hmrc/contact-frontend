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

object AccessibilityProblemReportPage extends BasePage with Helpers {

  val url: String            = wrapUrl("/accessibility")
  val pageTitle: String      = "Report an accessibility problem – GOV.UK"
  val welshPageTitle: String = "Rhoi gwybod am broblem hygyrchedd – GOV.UK"

  def problemDescription: TextArea = textArea("problemDescription")
  def nameField: TextField         = textField("name")
  def emailField: EmailField       = emailField("email")

  def completeReportForm(
    name: String = validName,
    email: String = validEmail,
    commentsLength: Int = 25
  ) = {
    nameField.value = name
    emailField.value = email
    problemDescription.value = generateRandomString(commentsLength)
  }

}

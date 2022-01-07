/*
 * Copyright 2022 HM Revenue & Customs
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

object ContactHmrcPage extends BasePage with Helpers {

  val url: String            = wrapUrl("/contact-hmrc")
  val pageTitle: String      = "Help and contact – GOV.UK"
  val welshPageTitle: String = "Cymorth a chysylltiadau – GOV.UK"

  def nameField: TextField    = textField("contact-name")
  def emailField: EmailField  = emailField("contact-email")
  def commentsField: TextArea = textArea("contact-comments")

  def completeReportForm(
    name: String = validName,
    email: String = validEmail,
    commentsLength: Int = 25
  ) = {
    nameField.value = name
    emailField.value = email
    commentsField.value = generateRandomString(commentsLength)
  }

}

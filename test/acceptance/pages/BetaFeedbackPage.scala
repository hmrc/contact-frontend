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

package acceptance.pages

import acceptance.support.Helpers

object BetaFeedbackPage extends BasePage with Helpers {

  val url: String = wrapUrl("/beta-feedback?service=pay")
  val pageTitle   = "Send your feedback – GOV.UK"

  def ratingRadioGroup        = radioButtonGroup("feedback-rating")
  def nameField: TextField    = textField("feedback-name")
  def emailField: EmailField  = emailField("feedback-email")
  def commentsField: TextArea = textArea("feedback-comments")

  def completeReportForm(
    name: String = validName,
    email: String = validEmail,
    rating: Int = 5,
    commentsLength: Int = 25
  ) = {
    nameField.value = name
    emailField.value = email
    ratingRadioGroup.value = rating.toString
    commentsField.value = generateRandomString(commentsLength)
  }

  def submitReportForm =
    click on CssSelectorQuery(".govuk-button[type=submit]")
}

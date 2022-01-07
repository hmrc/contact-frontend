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

object SurveyPage extends BasePage with Helpers {

  val url: String            = wrapUrl("/survey?ticketId=AAAA-AAAA-AAAA&serviceId=help-frontend")
  val pageTitle: String      = "Survey – GOV.UK"
  val welshPageTitle: String = "Arolwg – GOV.UK"

  def helpfulRadioGroup       = radioButtonGroup("helpful")
  def speedRadioGroup         = radioButtonGroup("speed")
  def commentsField: TextArea = textArea("improve")

  def completeReportForm(
    helpfulRating: Int = 5,
    speedRating: Int = 5,
    commentsLength: Int = 25
  ) = {
    helpfulRadioGroup.value = helpfulRating.toString
    speedRadioGroup.value = speedRating.toString
    commentsField.value = generateRandomString(commentsLength)
  }

}

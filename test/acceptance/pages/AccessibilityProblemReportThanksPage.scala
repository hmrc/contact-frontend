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

object AccessibilityProblemReportThanksPage extends BasePage {

  val url: String            = wrapUrl("/accessibility/thanks")
  val pageTitle: String      = "Report an accessibility problem – GOV.UK"
  val welshPageTitle: String = "Rhoi gwybod am broblem hygyrchedd – GOV.UK"

  val expectedHeading: String    = "Your accessibility problem has been reported."
  val expectedSubheading: String = "What happens next"

  def heading: String    = tagName("h1").element.text
  def subHeading: String = tagName("h2").element.text

}

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

object ReportProblemPage extends BasePage {

  val url: String = wrapUrl("/problem_reports_nonjs?service=pay")
  val title       = "Get help with a technical problem – GOV.UK"

  def nameField: TextField   = textField("report-name")
  def emailField: EmailField = emailField("report-email")
  def actionField: TextArea  = textArea("report-action")
  def errorField: TextArea   = textArea("report-error")

  def submitReportForm =
    click on CssSelectorQuery(".govuk-button[type=submit]")

  def completeReportForm = {
    nameField.value = "Rhiamon Tandy"
    emailField.value = "rtandy0@vinaora.com"
    actionField.value =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sollicitudin ex sit amet tristique dignissim. In tempus elit vehicula, faucibus odio sed, consequat odio. Curabitur cursus non leo et rutrum. Curabitur volutpat ultrices convallis. Integer ante metus, placerat at ante eu, facilisis fringilla quam. Donec sit amet diam ac ante elementum blandit. Cras consectetur sollicitudin sapien, nec accumsan nibh viverra ac.\n\nVestibulum sit amet neque est. Nullam vel ornare sapien. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quis leo rutrum, pretium nibh sit amet, fermentum turpis. Etiam tempus ex id auctor fermentum. Vivamus sed porttitor eros, vitae luctus nulla. Maecenas vel ipsum vel nibh convallis eleifend vel quis dolor. Nam at porta mi. Phasellus sed scelerisque tortor, a scelerisque massa. Duis in tortor sapien. Nulla elementum ultrices ligula a venenatis. Fusce odio erat, consequat sit amet leo sed, blandit sollicitudin lectus."
    errorField.value =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec ullamcorper tortor ac pellentesque viverra. Pellentesque venenatis sagittis lorem, at auctor."
  }
}

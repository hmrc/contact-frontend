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
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

trait TestOnlyReportProblemForm extends BasePage with Helpers {

  val pageTitle: String      = "Service test page – GOV.UK"
  val welshPageTitle: String = "Service test page – GOV.UK"
  val expectedSubHeading     = "Thank you"

  def nameField: TextField   = textField("report-name")
  def emailField: EmailField = emailField("report-email")
  def actionField: TextField = textField("report-action")
  def errorField: TextField  = textField("report-error")

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

  def problemReportIsHidden: Boolean = {
    val hidden = driver.findElements(By.xpath("//*[contains(@class, 'report-error') and contains(@class, 'hidden')]"))
    hidden.size().equals(1)
  }

  def problemReportIsLoaded: Boolean = {
    val formInput = driver.findElements(By.id("report-name"))
    formInput.size().equals(1)
  }

  def clickOnPageNotWorkingLink() = {
    click on id("get-help-action")
    val wait = new WebDriverWait(driver, 15)
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("report-name")))
  }

  def subHeading: String = tagName("h2").element.text

  override def submitForm() =
    click on CssSelectorQuery(".button[type=submit]")

}

object TestOnlyReportProblemPartialPage extends TestOnlyReportProblemForm {
  val url: String = wrapUrl("/test-only")
}

object TestOnlyReportProblemPartialPageAjax extends TestOnlyReportProblemForm {
  val url: String = wrapUrl("/test-only/ajax")
}

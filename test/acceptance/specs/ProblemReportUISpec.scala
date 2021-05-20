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

package acceptance.specs

import acceptance.pages.{ProblemReportPage, ThankYouPage}
import acceptance.specs.BaseSpec
import acceptance.specs.tags.{UiTests}

class ProblemReportUISpec extends BaseSpec {

  info("Happy path for reporting a problem")

  Feature("User reporting a problem") {

    Scenario("A user reports a problem", UiTests) {

      Given("A user is on the report a problem page")

      go to ProblemReportPage
      pageTitle shouldBe ProblemReportPage.title

      ProblemReportPage.completeProblemReportForm

      When("User submits the report")
      ProblemReportPage.submitProblemReportForm

      Then("The thank you page is displayed")
      eventually {
        pageTitle shouldBe ThankYouPage.title
      }

      ThankYouPage.heading    shouldBe ThankYouPage.expectedHeading
      ThankYouPage.subHeading shouldBe ThankYouPage.expectedSubHeading
    }
  }
}

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

import acceptance.pages.{ReportProblemPage, ReportProblemThanksPage}
import acceptance.specs.tags.UiTests

class ReportProblemSpec extends BaseSpec {

  info("UI tests for /contact/problem_reports_nonjs")

  Feature("Successfully submit a problem report form") {

    Scenario("I am able to successfully submit a problem report form", UiTests) {

      Given("I am on the report a problem page")
      go to ReportProblemPage
      pageTitle shouldBe ReportProblemPage.pageTitle

      When("I submit the report")
      ReportProblemPage.completeReportForm()
      ReportProblemPage.submitReportForm()

      Then("I see the thank you page")
      eventually {
        pageTitle shouldBe ReportProblemThanksPage.pageTitle
      }

      ReportProblemThanksPage.heading    shouldBe ReportProblemThanksPage.expectedHeading
      ReportProblemThanksPage.subHeading shouldBe ReportProblemThanksPage.expectedSubHeading
    }
  }
}

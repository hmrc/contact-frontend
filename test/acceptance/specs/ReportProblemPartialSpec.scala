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

package acceptance.specs

import acceptance.pages.{TestOnlyReportProblemPartialPage, TestOnlyReportProblemPartialPageAjax}
import acceptance.pages.TestOnlyReportProblemPartialPage._
import acceptance.specs.tags.UiTests
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.time.{Seconds, Span}

class ReportProblemPartialSpec extends BaseSpec with IntegrationPatience {

  private val timeoutInSeconds: Int                    = 2
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(timeoutInSeconds, Seconds)))

  info("UI tests for partial `Is this page not working properly` embedded form")

  Feature("Successfully submit a problem report form for server side form") {

    Scenario("I am able to successfully submit a problem report form", UiTests) {

      Given("I am on a service page with a hidden embedded form")
      go to TestOnlyReportProblemPartialPage
      pageTitle             shouldBe TestOnlyReportProblemPartialPage.pageTitle
      problemReportIsHidden shouldBe true

      When("I click the `Is this page not working properly` link")
      TestOnlyReportProblemPartialPage.clickOnPageNotWorkingLink()

      And("I see the get help with technical problem form")
      problemReportIsHidden shouldBe false

      And("I submit the report")
      TestOnlyReportProblemPartialPage.completeReportForm()
      TestOnlyReportProblemPartialPage.submitForm()

      Then("I see the thank you message in the page")
      eventually {
        TestOnlyReportProblemPartialPage.subHeading shouldBe TestOnlyReportProblemPartialPage.expectedSubHeading
      }
    }
  }

//  Feature("Successfully submit a problem report form for ajax form") {
//
//    Scenario("I am able to successfully submit a problem report form", UiTests) {
//
//      Given("I am on a service page with a link to retrieve an async form")
//      go to TestOnlyReportProblemPartialPageAjax
//      pageTitle             shouldBe TestOnlyReportProblemPartialPageAjax.pageTitle
//      problemReportIsLoaded shouldBe false
//
//      When("I click the `Is this page not working properly` link")
//      TestOnlyReportProblemPartialPageAjax.clickOnPageNotWorkingLink()
//
//      And("I see the get help with technical problem form")
//      problemReportIsLoaded shouldBe true
//
//      And("I submit the report")
//      TestOnlyReportProblemPartialPageAjax.completeReportForm()
//      TestOnlyReportProblemPartialPageAjax.submitForm()
//
//      Then("I see the thank you message in the page")
//      eventually {
//        TestOnlyReportProblemPartialPageAjax.subHeading shouldBe TestOnlyReportProblemPartialPageAjax.expectedSubHeading
//      }
//    }
//  }
}

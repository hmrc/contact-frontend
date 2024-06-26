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

import acceptance.driver.BrowserDriver
import org.scalatest.*
import org.scalatest.concurrent.Eventually
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.selenium.WebBrowser
import support.AcceptanceTestServer
import uk.gov.hmrc.selenium.webdriver.{Browser, ScreenshotOnFailure}

trait BaseSpec
    extends AnyFeatureSpec
    with GivenWhenThen
    with BeforeAndAfterEach
    with Matchers
    with Browser
    with ScreenshotOnFailure
    with WebBrowser
    with AcceptanceTestServer
    with BrowserDriver
    with Eventually {

  override def beforeEach(): Unit =
    startBrowser()

  override def afterEach(): Unit =
    quitBrowser()

}

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

package helpers

import config.AppConfig
import play.api.mvc.Request
import util.Feature

class TestAppConfig extends AppConfig {
  def contactHmrcAboutTaxUrl: String = "some.contact.url"

  def externalReportProblemUrl: String = ???

  def externalReportProblemSecureUrl: String = ???

  def backUrlDestinationAllowList: Set[String] = ???

  def loginCallback(continueUrl: String): String = ???

  def enableLanguageSwitching: Boolean = true

  def enablePlayFrontendAccessibilityForm: Boolean = false

  def enablePlayFrontendFeedbackForm: Boolean = false

  def enablePlayFrontendProblemReportNonjsForm: Boolean = false

  def enablePlayFrontendSurveyForm: Boolean = false

  def enablePlayFrontendContactHmrcForm: Boolean = false

  def hasFeature(f: Feature, service: Option[String])(implicit request: Request[_]): Boolean = ???

  def getFeatures(service: Option[String])(implicit request: Request[_]): Set[Feature] = ???

}

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

package model

import Aliases.*

// Type aliases to suppress PR-commenter warnings around potential open redirects
object Aliases {
  // These backUrls are already validated against an allow-list by the BackUrlValidator
  type BackUrl     = String
  // These referrerUrls aren't used to redirect anywhere - they're just passed along to Deskpro for information
  type ReferrerUrl = String
}

case class AccessibilityForm(
  problemDescription: String,
  name: String,
  email: String,
  isJavascript: Boolean,
  referrer: ReferrerUrl,
  csrfToken: String,
  service: Option[String] = Some("unknown"),
  userAction: Option[String] = None
)

case class ReportProblemForm(
  reportName: String,
  reportEmail: String,
  reportAction: String,
  reportError: String,
  isJavascript: Boolean,
  service: Option[String],
  referrer: Option[ReferrerUrl],
  csrfToken: String,
  userAction: Option[String]
)

case class FeedbackForm(
  experienceRating: Option[String],
  name: String,
  email: String,
  comments: String,
  javascriptEnabled: Boolean,
  referrer: ReferrerUrl,
  csrfToken: String,
  service: Option[String],
  backUrl: Option[BackUrl],
  canOmitComments: Boolean
)

object FeedbackFormConfig {
  val validExperiences: Seq[String] = (5 to 1 by -1) map (_.toString)
}

case class SurveyForm(
  helpful: Option[Int],
  speed: Option[Int],
  improve: Option[String],
  ticketId: Option[String],
  serviceId: Option[String]
)

case class DateOfBirth(day: String, month: String, year: String)

object DateOfBirth {
  val empty: DateOfBirth = DateOfBirth("", "", "")
}

case class ReportOneLoginProblemForm(
  name: String,
  nino: String,
  saUtr: Option[String],
  dateOfBirth: DateOfBirth,
  email: String,
  phoneNumber: Option[String],
  address: String,
  contactPreference: Option[String],
  complaint: Option[String],
  csrfToken: String
)

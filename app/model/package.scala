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
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.language.Language
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.reporttechnicalissue.ReportTechnicalIssue

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

object AccessibilityForm {
  def apply(
    problemDescription: String,
    name: String,
    email: String,
    isJavascript: Boolean,
    referrer: ReferrerUrl,
    csrfToken: String,
    service: Option[String],
    userAction: Option[String]
  ): AccessibilityForm = new AccessibilityForm(
    problemDescription,
    name,
    email,
    isJavascript,
    referrer,
    csrfToken,
    service,
    userAction
  )

  def unapply(form: AccessibilityForm): Option[
    (
      String,
      String,
      String,
      Boolean,
      String,
      String,
      Option[String],
      Option[String]
    )
  ] = Some(
    (
      form.problemDescription,
      form.name,
      form.email,
      form.isJavascript,
      form.referrer,
      form.csrfToken,
      form.service,
      form.userAction
    )
  )
}

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

object ReportProblemForm {
  def apply(
    reportName: String,
    reportEmail: String,
    reportAction: String,
    reportError: String,
    isJavascript: Boolean,
    service: Option[String],
    referrer: Option[ReferrerUrl],
    csrfToken: String,
    userAction: Option[String]
  ): ReportProblemForm = new ReportProblemForm(
    reportName,
    reportEmail,
    reportAction,
    reportError,
    isJavascript,
    service,
    referrer,
    csrfToken,
    userAction
  )

  def unapply(form: ReportProblemForm): Option[
    (
      String,
      String,
      String,
      String,
      Boolean,
      Option[String],
      Option[String],
      String,
      Option[String]
    )
  ] = Some(
    (
      form.reportName,
      form.reportEmail,
      form.reportAction,
      form.reportError,
      form.isJavascript,
      form.service,
      form.referrer,
      form.csrfToken,
      form.userAction
    )
  )
}

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

object FeedbackForm {
  def apply(
    experienceRating: Option[String],
    name: String,
    email: String,
    comments: String,
    javascriptEnabled: Boolean,
    referrer: ReferrerUrl,
    csrfToken: String,
    service: Option[String],
    backUrl: Option[String],
    canOmitComments: Boolean
  ): FeedbackForm =
    FeedbackForm(
      experienceRating,
      name,
      email,
      comments,
      javascriptEnabled,
      referrer,
      csrfToken,
      service,
      backUrl,
      canOmitComments
    )

  def unapply(form: FeedbackForm): Option[
    (
      Option[String],
      String,
      String,
      String,
      Boolean,
      String,
      String,
      Option[String],
      Option[String],
      Boolean
    )
  ] = Some(
    (
      form.experienceRating,
      form.name,
      form.email,
      form.comments,
      form.javascriptEnabled,
      form.referrer,
      form.csrfToken,
      form.service,
      form.backUrl,
      form.canOmitComments
    )
  )
}

object FeedbackFormConfig {
  val validExperiences = (5 to 1 by -1) map (_.toString)
}

case class SurveyForm(
  helpful: Option[Int],
  speed: Option[Int],
  improve: Option[String],
  ticketId: Option[String],
  serviceId: Option[String]
)

object SurveyForm {
  def apply(
    helpful: Option[Int],
    speed: Option[Int],
    improve: Option[String],
    ticketId: Option[String],
    serviceId: Option[String]
  ): SurveyForm = new SurveyForm(
    helpful,
    speed,
    improve,
    ticketId,
    serviceId
  )

  def unapply(form: SurveyForm): Option[
    (
      Option[Int],
      Option[Int],
      Option[String],
      Option[String],
      Option[String]
    )
  ] = Some(
    (
      form.helpful,
      form.speed,
      form.improve,
      form.ticketId,
      form.serviceId
    )
  )
}

object ReportTechnicalIssue {
  def apply(
    serviceId: String,
    language: Language
  ): ReportTechnicalIssue = new ReportTechnicalIssue(
    serviceId,
    "",
    language,
    None,
    None,
    None
  )
}

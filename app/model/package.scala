/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package model

case class AccessibilityForm(
  problemDescription: String,
  name: String,
  email: String,
  isJavascript: Boolean,
  referrer: String,
  csrfToken: String,
  service: Option[String] = Some("unknown"),
  userAction: Option[String] = None
)

case class ProblemReport(
  reportName: String,
  reportEmail: String,
  reportAction: String,
  reportError: String,
  isJavascript: Boolean,
  service: Option[String],
  abFeatures: Option[String],
  referrer: Option[String],
  userAction: Option[String]
)

case class FeedbackForm(
  experienceRating: Option[String],
  name: String,
  email: String,
  comments: String,
  javascriptEnabled: Boolean,
  referrer: String,
  csrfToken: String,
  service: Option[String],
  abFeatures: Option[String] = None,
  backUrl: Option[String],
  canOmitComments: Boolean
)

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

package model

case class AccessibilityForm(
  problemDescription: String,
  name: String,
  email: String,
  isJavascript: Boolean,
  referrer: String,
  csrfToken: String,
  service: Option[String] = Some("unknown"),
  userAction: Option[String] = None)

case class ProblemReport(
  reportName: String,
  reportEmail: String,
  reportAction: String,
  reportError: String,
  isJavascript: Boolean,
  service: Option[String],
  abFeatures: Option[String],
  referrer: Option[String],
  userAction: Option[String])

case class FeedbackForm(
  experienceRating: String,
  name: String,
  email: String,
  comments: String,
  javascriptEnabled: Boolean,
  referrer: String,
  csrfToken: String,
  service: Option[String]    = Some("unknown"),
  abFeatures: Option[String] = None,
  backUrl: Option[String],
  canOmitComments: Boolean)

object FeedbackForm {
  def apply(referer: String, csrfToken: String, backUrl: Option[String], canOmitComments: Boolean): FeedbackForm =
    FeedbackForm(
      "",
      "",
      "",
      "",
      javascriptEnabled = false,
      referrer          = referer,
      csrfToken         = csrfToken,
      backUrl           = backUrl,
      canOmitComments   = canOmitComments)

  def apply(
    experienceRating: Option[String],
    name: String,
    email: String,
    comments: String,
    javascriptEnabled: Boolean,
    referrer: String,
    csrfToken: String,
    service: Option[String],
    abFeatures: Option[String],
    backUrl: Option[String],
    canOmitComments: Boolean
  ): FeedbackForm =
    FeedbackForm(
      experienceRating.getOrElse(""),
      name,
      email,
      comments,
      javascriptEnabled,
      referrer,
      csrfToken,
      service,
      abFeatures,
      backUrl,
      canOmitComments)

}

object FeedbackFormConfig {
  val validExperiences = (5 to 1 by -1) map (_.toString)
}

package controllers

import play.api.Play
import uk.gov.hmrc.play.config.RunMode

object ExternalUrls extends RunMode {
  import play.api.Play.current

  val ytaHost = Play.configuration.getString(s"govuk-tax.$env.yta.host").getOrElse("")

  val businessTaxHome = s"$ytaHost/account"
  val reportProblemUrl = s"${config.ExternalUrls.contactHost}/contact/problem_reports"

}

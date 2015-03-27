package config

import play.twirl.api.Html
import uk.gov.hmrc.play.config.FrontendGlobal

object ContactFrontendGlobal extends FrontendGlobal {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String): Html =
    views.html.error_template(pageTitle, heading, message)
}

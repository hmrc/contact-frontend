package config

import config.FrontendGlobal
import play.twirl.api.Html

object ContactFrontendGlobal extends FrontendGlobal {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String): Html =
    views.html.error_template(pageTitle, heading, message)
}

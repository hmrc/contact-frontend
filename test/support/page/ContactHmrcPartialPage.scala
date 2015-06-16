package support.page

import support.steps.Env

class ContactHmrcPartialPage(submitUrl: String, service: Option[String] = None) extends ContactHmrcPage {

  override val url = Env.host + s"/contact/contact-hmrc/form?submitUrl=$submitUrl&csrfToken=token&service=${service.getOrElse("")}"

}
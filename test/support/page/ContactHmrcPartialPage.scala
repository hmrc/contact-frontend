package support.page

import support.steps.Env

class ContactHmrcPartialPage(submitUrl: String) extends ContactHmrcPage {

  override val url = Env.host + s"/contact/contact-hmrc/form?submitUrl=$submitUrl&csrfToken=token"

}
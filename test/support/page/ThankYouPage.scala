package support.page

import support.modules.SharedPageModules
import support.steps.Env

object ThankYouPage  extends WebPage with SharedPageModules {
  override val url = Env.host + "/contact/???"

  override def isCurrentPage: Boolean = heading=="Thank you"
}

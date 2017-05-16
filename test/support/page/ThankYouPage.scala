package support.page

import support.modules.SharedPageModules
import support.steps.Env

object ThankYouPage  extends WebPage with SharedPageModules {
  override val url: String = Env.host + "/contact/???"

  override def title: String = "Thank you"
  override def isCurrentPage: Boolean = heading == title
}

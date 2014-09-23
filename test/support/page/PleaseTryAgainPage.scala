package support.page

import support.modules.SharedPageModules
import support.steps.Env

object PleaseTryAgainPage  extends WebPage with SharedPageModules {
  override val url = Env.host + "/contact/???"

  override def isCurrentPage: Boolean = heading=="Please try again"
}

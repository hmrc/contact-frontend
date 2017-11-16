package support.page

import support.modules.SharedPageModules
import support.util.Env

object PleaseTryAgainPage  extends WebPage with SharedPageModules {
  override val url = Env.host + "/contact/???"

  override def isCurrentPage: Boolean = heading=="There was a problem sending your query"
}

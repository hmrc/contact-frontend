package support.page

import support.modules.SharedPageModules
import support.steps.Env

object PleaseTryAgainPage  extends WebPage with SharedPageModules {
  override val url: String = Env.host + "/contact/???"

  override def title: String = "There was a problem sending your query"
  override def isCurrentPage: Boolean = heading == title
}

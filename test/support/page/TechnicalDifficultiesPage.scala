package support.page

import support.modules.SharedPageModules
import support.steps.Env

object TechnicalDifficultiesPage  extends WebPage with SharedPageModules {
  override val url: String = Env.host + "/contact/???"

  override def title: String = "Sorry, we’re experiencing technical difficulties"
  override def isCurrentPage: Boolean = heading == title
}

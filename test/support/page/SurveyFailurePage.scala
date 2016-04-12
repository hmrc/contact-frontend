package support.page

import support.modules.SharedPageModules
import support.steps.Env

trait SurveyFailurePage extends WebPage with SharedPageModules {

  override val url = Env.host + "/contact/survey/failure"

  override def isCurrentPage: Boolean = heading=="Error submitting feedback; please try again"
}

object SurveyFailurePage extends SurveyFailurePage

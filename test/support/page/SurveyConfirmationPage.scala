package support.page

import support.modules.SharedPageModules
import support.steps.Env

trait SurveyConfirmationPage extends WebPage with SharedPageModules {

  override val url = Env.host + "/contact/survey/confirmation"

  override def isCurrentPage: Boolean = heading=="Thank you, your feedback has been received"
}

object SurveyConfirmationPage extends SurveyConfirmationPage

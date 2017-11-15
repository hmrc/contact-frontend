package support.page

import support.modules.SharedPageModules
import support.util.Env

trait SurveyConfirmationPage extends WebPage with SharedPageModules {

  override val url = Env.host + "/contact/survey/confirmation"

  override def isCurrentPage: Boolean = heading=="Thank you, your feedback has been received"
}

object SurveyConfirmationPage extends SurveyConfirmationPage


trait SurveyConfirmationPageWelsh extends SurveyConfirmationPage {

  override val url = Env.host + "/contact/survey/confirmation"

  override def isCurrentPage: Boolean = heading=="Diolch, mae eich adborth wedi dod i law."
}

object SurveyConfirmationPageWelsh extends SurveyConfirmationPageWelsh

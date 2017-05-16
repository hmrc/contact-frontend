package support.page

import support.modules.SharedPageModules
import support.steps.Env

trait SurveyConfirmationPage extends WebPage with SharedPageModules {

  override val url: String = Env.host + "/contact/survey/confirmation"

  override def title: String = "Thank you, your feedback has been received"
  override def isCurrentPage: Boolean = heading == title
}

object SurveyConfirmationPage extends SurveyConfirmationPage


trait SurveyConfirmationPageWelsh extends SurveyConfirmationPage {

  override val url: String = Env.host + "/contact/survey/confirmation"

  override def title: String = "Diolch, mae eich adborth wedi dod i law."
  override def isCurrentPage: Boolean = heading == title
}

object SurveyConfirmationPageWelsh extends SurveyConfirmationPageWelsh

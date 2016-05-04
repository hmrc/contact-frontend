package support.page

import org.openqa.selenium.By
import support.modules.SharedPageModules
import support.steps.Env

trait SurveyPage extends WebPage with SharedPageModules {

  override val url = Env.host + "/contact/survey"

  override def isCurrentPage: Boolean = heading=="Survey"


  def selectHowHelpfulTheResponseWas(option: String) {
    click on id("helpful-"+option)
  }

  def selectHowSpeedyTheResponseWas(option: String) {
    click on id("speed-"+option)
  }

  def setAdditionalComment(reason: String) {
    webDriver.findElement(By.id("improve")).sendKeys(reason)
  }

  def clickSubmitButton() = click on id("submit-survey-button")
}

object SurveyPage extends SurveyPage

class SurveyPageWithTicketAndServiceIds(ticketId: String, serviceId:String) extends SurveyPage {

  override val url = Env.host + s"/contact/survey?ticketId=$ticketId&serviceId=$serviceId"//&csrfToken=token"
}
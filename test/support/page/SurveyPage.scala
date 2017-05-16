package support.page

import org.openqa.selenium.By
import support.modules.SharedPageModules
import support.steps.Env

trait SurveyPage extends WebPage with SharedPageModules {

  override val url: String = Env.host + "/contact/survey"

  override def title: String = "Survey"
  override def isCurrentPage: Boolean = heading == title


  def selectHowHelpfulTheResponseWas(option: String) {
    click on id("helpful-"+option)
  }

  def selectHowSpeedyTheResponseWas(option: String) {
    click on id("speed-"+option)
  }

  def setAdditionalComment(reason: String) {
    webDriver.findElement(By.id("improve")).sendKeys(reason)
  }

  def clickSubmitButton(): Unit = click on id("submit-survey-button")
}

object SurveyPage extends SurveyPage

class SurveyPageWithTicketAndServiceIds(ticketId: String, serviceId:String) extends SurveyPage {
  override val url: String = Env.host + s"/contact/survey?ticketId=$ticketId&serviceId=$serviceId"
}
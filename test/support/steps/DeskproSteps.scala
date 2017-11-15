package support.steps

import play.api.libs.json.{JsString, Json}
import support.behaviour.NavigationSugar
import support.modules.Deskpro

trait DeskproSteps extends NavigationSugar with BaseSteps{

  def ticket_is_created_in_deskpro(ccsTicketId: String, name: String, email: String, textInMessageBody: Seq[String]) = {
    val deskProTicketId = ticket_is_created_in_hmrc_deskpro(ccsTicketId)
    val ticketDetails: String = Deskpro.getTicketDetailsFor(deskProTicketId)
    ticketDetails shouldNot be("")
    val responseBody = (Json.parse(ticketDetails) \ "messages")(0)
    (responseBody \ "person" \ "display_name").as[JsString].value should be(name)
    val emailActual = (responseBody \ "person" \ "primary_email" \ "email").as[JsString].value
    (responseBody \ "person" \ "primary_email" \ "email").as[JsString].value should be(email)
    (responseBody \ "message").as[JsString].value should containInOrder(textInMessageBody)
  }

  def ticket_is_created_in_hmrc_deskpro(ccsTicketId: String): String = {
    Thread.sleep(60000)
    val deskProTicketId: String = HMRCDeskpro.getDeskproTicketIdFrom(ccsTicketId)
    deskProTicketId shouldNot be("null")
    deskProTicketId
  }
}

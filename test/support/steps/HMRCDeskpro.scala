package support.steps

import play.api.libs.json.Json
import support.util.HmrcHttpClient

object HMRCDeskpro {
  val sampleResponse = "{\"ccs_ticket_id\":50,\"deskpro_ticket_id\":464625}"
  def getDeskproTicketIdFrom(ccsTicketId: String): String = {
//    running tests from local will need a different base url than running in jenkins
//    for when the tests are running on jenkins
//    val responseString  = HmrcHttpClient.getResponseFor(s"${Env.hMRCDeskproBaseUrl}/ticket?ccsTicketId=${ccsTicketId}")
//    for when the tests are running on local
    val responseString  = HmrcHttpClient.getResponseFor(s"https://www.qa.tax.service.gov.uk/deskpro/ticket?ccsTicketId=${ccsTicketId}")
    (Json.parse(responseString) \ "deskpro_ticket_id").get.toString()
//    (Json.parse(sampleResponse) \ "deskpro_ticket_id").as[JsString].value
//    ""
  }
}
package support.modules

import org.apache.http.message.BasicHeader
import support.util.{Env, HmrcHttpClient}


object Deskpro {
  val apiKeyHeader : BasicHeader  = new BasicHeader("X-DeskPRO-API-Key","79:PMT6R9NHYA629CA5CJ7SACYQ9")
  def getTicketDetailsFor(deskProTicketId: String): String = {
    val response = HmrcHttpClient.getResponseFor(s"${Env.stagingDeskproBaseUrl}/api/tickets/${deskProTicketId}/messages", Array(apiKeyHeader))
    response
  }

}

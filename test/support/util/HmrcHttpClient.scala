package support.util

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.apache.http.{Header, HttpStatus}


object HmrcHttpClient {
  def getResponseFor(url: String, headers: Array[Header] = Array()): String = {
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    val httpGet: HttpGet = new HttpGet(url)
    httpGet.setHeaders(headers)
    val httpResponse = httpClient.execute(httpGet)
    if (httpResponse.getStatusLine.getStatusCode != HttpStatus.SC_OK) {
      return ""
    }
    val responseString = EntityUtils.toString(httpResponse.getEntity)
    EntityUtils.consume(httpResponse.getEntity)
    responseString
  }

}

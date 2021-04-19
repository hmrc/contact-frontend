/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test

import java.net.ServerSocket
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.util.Try
import collection.JavaConverters._

trait WireMockEndpoints extends Suite with BeforeAndAfterAll with BeforeAndAfterEach {

  val host: String = "localhost"

  val endpointPort: Int              = PortTester.findPort()
  val endpointMock                   = new WireMock(host, endpointPort)
  val endpointMockUrl                = s"http://$host:$endpointPort"
  val endpointServer: WireMockServer = new WireMockServer(wireMockConfig().port(endpointPort))

  def startWireMock() = endpointServer.start()
  def stopWireMock()  = endpointServer.stop()

  override def beforeEach(): Unit = {
    endpointMock.resetMappings()
    endpointMock.resetScenarios()
    endpointServer.stubFor(
      post(urlEqualTo("/deskpro/get-help-ticket"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{ "ticket_id": 12345 }""".stripMargin)
        )
    )
    endpointServer.stubFor(
      post(urlEqualTo("/deskpro/feedback"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{ "ticket_id": 12345 }""".stripMargin)
        )
    )
  }
  override def afterAll(): Unit  =
    endpointServer.stop()
  override def beforeAll(): Unit =
    endpointServer.start()

  def printMappings(): Unit =
    asScalaBuffer(endpointMock.allStubMappings().getMappings).foreach { s =>
      println(s)
    }

}

object PortTester {

  def findPort(excluded: Int*): Int =
    (6001 to 7000).find(port => !excluded.contains(port) && isFree(port)).getOrElse(throw new Exception("No free port"))

  private def isFree(port: Int): Boolean = {
    val triedSocket = Try {
      val serverSocket = new ServerSocket(port)
      Try(serverSocket.close())
      serverSocket
    }
    triedSocket.isSuccess
  }
}

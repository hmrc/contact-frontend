/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import connectors.deskpro.DeskproTicketQueueConnector
import connectors.deskpro.domain.{BetaFeedbackTicketConstants, ReportTechnicalProblemTicketConstants, TicketId}
import org.mockito.Mockito.when
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
//import org.mockito.MockitoSugar.when
//import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import test.helpers.{AwaitSupport, WireMockEndpoints}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, AuditResult, DatastreamMetrics}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class DeskproTicketQueueConnectorSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with AwaitSupport
    with WireMockEndpoints {

  val mockAuditConnector = mock[AuditConnector]

  val auditConnector = new AuditConnector {
    val mockAuditingConfig = mock[AuditingConfig]
    when(mockAuditingConfig.enabled).thenReturn(true)

    override def auditingConfig: AuditingConfig       = mockAuditingConfig
    override def auditChannel: AuditChannel           = ???
    override def datastreamMetrics: DatastreamMetrics = ???

    override def sendExtendedEvent(event: ExtendedDataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
      mockAuditConnector.sendExtendedEvent(event)
    }
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"                       -> false,
        "metrics.enabled"                         -> false,
        "microservice.services.deskpro-ticket-queue.port" -> endpointPort,
        "sendExplicitAuditEvents" -> true
      )
      .overrides(
        bind(classOf[AuditConnector]).toInstance(auditConnector)
      )
      .build()

  "createProblemReportsTicket" should {
    "POST standard enrolments" in new Setup {

      await(
        createTicket(
          Enrolments(enrolments)
        )
      )

      val problemReportsRequestJson = """{
                          |"name":"Mary",
                          |"email":"mary@example.com",
                          |"subject":"Support Request",
                          |"message":"A problem occurred",
                          |"referrer":"",
                          |"javascriptEnabled":"Y",
                          |"userAgent":"n/a",
                          |"authId":"n/a",
                          |"areaOfTax":"unknown",
                          |"sessionId":"n/a",
                          |"userTaxIdentifiers":{
                          |  "nino":"SOME-NINO",
                          |  "ctUtr":"SOME-CT-UTR",
                          |  "utr":"SOME-SA-UTR",
                          |  "vrn":"SOME-VATDEC-VATRegNo",
                          |  "empRef":"SOME-TaxOfficeNumber/SOME-TaxOfficeReference"
                          |},
                          |"service":"example-frontend"
                          |}""".stripMargin

      endpointServer.verify(
        postRequestedFor(urlEqualTo("/deskpro/get-help-ticket"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(problemReportsRequestJson)
          )
      )

      val explicitAuditEvents = captureAuditEvents().filter(_.auditType == "ReportTechnicalProblemFormSubmission")
      explicitAuditEvents.length shouldBe 1
      explicitAuditEvents.head.detail shouldBe Json.parse(problemReportsRequestJson)
    }

    "POST additional enrolments" in new Setup {

      await(
        createTicket(
          Enrolments(enrolments ++ additionalEnrolments)
        )
      )

      endpointServer.verify(
        postRequestedFor(urlEqualTo("/deskpro/get-help-ticket"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(
              """{
                |"name":"Mary",
                |"email":"mary@example.com",
                |"subject":"Support Request",
                |"message":"A problem occurred",
                |"referrer":"",
                |"javascriptEnabled":"Y",
                |"userAgent":"n/a",
                |"authId":"n/a",
                |"areaOfTax":"unknown",
                |"sessionId":"n/a",
                |"userTaxIdentifiers":{
                |  "nino":"SOME-NINO",
                |  "ctUtr":"SOME-CT-UTR",
                |  "utr":"SOME-SA-UTR",
                |  "vrn":"SOME-VATDEC-VATRegNo",
                |  "empRef":"SOME-TaxOfficeNumber/SOME-TaxOfficeReference",
                |  "HMCE-VAT-AGNT/AgentRefNo":"FOO",
                |  "HMRC-AWRS-ORG/AWRSRefNumber":"BAR"
                |},
                |"service":"example-frontend"
                |}""".stripMargin
            )
          )
      )
    }
  }

  "createFeedback" should {
    "POST standard enrolments" in new Setup {

      await(
        createFeedback(
          Enrolments(enrolments)
        )
      )

      val feedbackRequestJson = """{
                                  |"name":"Eric",
                                  |"email":"eric@example.com",
                                  |"subject":"Beta feedback submission",
                                  |"message":"No comment given",
                                  |"referrer":"",
                                  |"javascriptEnabled":"Y",
                                  |"userAgent":"n/a",
                                  |"authId":"n/a",
                                  |"areaOfTax":"unknown",
                                  |"sessionId":"n/a",
                                  |"userTaxIdentifiers":{
                                  |  "nino":"SOME-NINO",
                                  |  "ctUtr":"SOME-CT-UTR",
                                  |  "utr":"SOME-SA-UTR",
                                  |  "vrn":"SOME-VATDEC-VATRegNo",
                                  |  "empRef":"SOME-TaxOfficeNumber/SOME-TaxOfficeReference"
                                  |},
                                  |"service":"example-frontend",
                                  |"rating":"4"
                                  |}""".stripMargin


      endpointServer.verify(
        postRequestedFor(urlEqualTo("/deskpro/feedback"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(feedbackRequestJson)
          )
      )

      val explicitAuditEvents = captureAuditEvents().filter(_.auditType == "BetaFeedbackFormSubmission")
      explicitAuditEvents.length shouldBe 1
      explicitAuditEvents.head.detail shouldBe Json.parse(feedbackRequestJson)
    }

    "POST additional enrolments" in new Setup {

      await(
        createFeedback(
          Enrolments(enrolments ++ additionalEnrolments)
        )
      )

      endpointServer.verify(
        postRequestedFor(urlEqualTo("/deskpro/feedback"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(
              """{
                |"name":"Eric",
                |"email":"eric@example.com",
                |"subject":"Beta feedback submission",
                |"message":"No comment given",
                |"referrer":"",
                |"javascriptEnabled":"Y",
                |"userAgent":"n/a",
                |"authId":"n/a",
                |"areaOfTax":"unknown",
                |"sessionId":"n/a",
                |"userTaxIdentifiers":{
                |  "nino":"SOME-NINO",
                |  "ctUtr":"SOME-CT-UTR",
                |  "utr":"SOME-SA-UTR",
                |  "vrn":"SOME-VATDEC-VATRegNo",
                |  "empRef":"SOME-TaxOfficeNumber/SOME-TaxOfficeReference",
                |  "HMCE-VAT-AGNT/AgentRefNo":"FOO",
                |  "HMRC-AWRS-ORG/AWRSRefNumber":"BAR"
                |},
                |"service":"example-frontend",
                |"rating":"4"
                |}""".stripMargin
            )
          )
      )
    }
  }

  class Setup {
    def ticketQueueConnector = app.injector.instanceOf[DeskproTicketQueueConnector]

    implicit val request: Request[AnyRef] = FakeRequest()

    def createTicket(enrolments: Enrolments): Future[TicketId] = {
      implicit val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      ticketQueueConnector.createDeskProTicket(
        name = "Mary",
        email = "mary@example.com",
        message = "A problem occurred",
        referrer = "",
        isJavascript = true,
        request = request,
        enrolmentsOption = Some(enrolments),
        service = Some("example-frontend"),
        userAction = None,
        ticketConstants = ReportTechnicalProblemTicketConstants
      )
    }

    def createFeedback(enrolments: Enrolments): Future[TicketId] = {
      implicit val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      ticketQueueConnector.createFeedback(
        name = "Eric",
        email = "eric@example.com",
        rating = "4",
        message = "No comment given",
        referrer = "",
        isJavascript = true,
        request = request,
        enrolmentsOption = Some(enrolments),
        service = Some("example-frontend"),
        ticketConstants = BetaFeedbackTicketConstants
      )
    }

    val enrolments =
      Set(
        Enrolment(
          key = "HMRC-NI",
          identifiers = Seq(EnrolmentIdentifier(key = "NINO", value = "SOME-NINO")),
          state = ""
        ),
        Enrolment(
          key = "IR-SA",
          identifiers = Seq(EnrolmentIdentifier(key = "UTR", value = "SOME-SA-UTR")),
          state = ""
        ),
        Enrolment(
          key = "IR-CT",
          identifiers = Seq(EnrolmentIdentifier(key = "UTR", value = "SOME-CT-UTR")),
          state = ""
        ),
        Enrolment(
          key = "HMCE-VATDEC-ORG",
          identifiers = Seq(EnrolmentIdentifier(key = "VATRegNo", value = "SOME-VATDEC-VATRegNo")),
          state = ""
        ),
        Enrolment(
          key = "IR-PAYE",
          identifiers = Seq(
            EnrolmentIdentifier(key = "TaxOfficeNumber", value = "SOME-TaxOfficeNumber"),
            EnrolmentIdentifier(key = "TaxOfficeReference", value = "SOME-TaxOfficeReference")
          ),
          state = ""
        )
      )

    val additionalEnrolments =
      Set(
        Enrolment(
          key = "HMCE-VAT-AGNT",
          identifiers = Seq(EnrolmentIdentifier(key = "AgentRefNo", value = "FOO")),
          state = ""
        ),
        Enrolment(
          key = "HMRC-AWRS-ORG",
          identifiers = Seq(EnrolmentIdentifier(key = "AWRSRefNumber", value = "BAR")),
          state = ""
        )
      )
  }

  private def captureAuditEvents() = {
    val eventCaptor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
    Mockito
      .verify(mockAuditConnector, Mockito.atLeast(0))
      .sendExtendedEvent(eventCaptor.capture())(ArgumentMatchers.any(), ArgumentMatchers.any())

    eventCaptor.getAllValues.asScala
  }
}

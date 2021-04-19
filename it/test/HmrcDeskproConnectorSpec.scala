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

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import connectors.deskpro.HmrcDeskproConnector
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.play.HeaderCarrierConverter

class HmrcDeskproConnectorSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with AwaitSupport
    with WireMockEndpoints {
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"                             -> false,
        "metrics.enabled"                         -> false,
        "auditing.enabled"                        -> false,
        "microservice.services.hmrc-deskpro.port" -> endpointPort
      )
      .build()

  "createProblemReportsTicket" should {
    "POST standard enrolments" in new Setup {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

      implicit val messages = messagesApi.preferred(request)

      await(
        createTicket(
          Enrolments(enrolments)
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
                |  "empRef":"SOME-TaxOfficeNumber/SOME-TaxOfficeReference"
                |},
                |"service":"example-frontend"
                |}""".stripMargin
            )
          )
      )
    }

    "POST additional enrolments" in new Setup {
      val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

      implicit val messages = messagesApi.preferred(request)

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

  class Setup {
    def hmrcDeskproConnector = app.injector.instanceOf[HmrcDeskproConnector]

    implicit val request: Request[AnyRef] = FakeRequest()

    def createTicket(enrolments: Enrolments) = {
      implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      hmrcDeskproConnector.createDeskProTicket(
        name = "Mary",
        email = "mary@example.com",
        subject = "Support Request",
        message = "A problem occurred",
        referrer = "",
        isJavascript = true,
        request = request,
        enrolmentsOption = Some(enrolments),
        service = Some("example-frontend"),
        userAction = None
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
}

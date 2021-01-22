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

package connectors.deskpro.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys, UserId}

class FieldTransformerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false, "metrics.enabled" -> false)
      .build()

  "Field Transformer" should {

    "transform javascript not enabled" in new FieldTransformerScope {
      transformer.ynValueOf(javascript = false) shouldBe "N"
    }

    "transform javascript  enabled" in new FieldTransformerScope {
      transformer.ynValueOf(javascript = true) shouldBe "Y"
    }

    "use area of tax as unknown" in new FieldTransformerScope {
      transformer.areaOfTaxOf shouldBe "unknown"
    }

    "transform userId to AuthenticatedUser if auth token present in session" in new FieldTransformerScope {
      transformer.userIdFrom(requestAuthenticatedByIda) shouldBe "AuthenticatedUser"
    }

    "transforms no auth token in the session to userId n/a" in new FieldTransformerScope {
      transformer.userIdFrom(FakeRequest()) shouldBe "n/a"
    }

    "transform  sessionId in the header carrier to session id" in new FieldTransformerScope {
      transformer.sessionIdFrom(hc) shouldBe sessionId
    }

    "transform no sessionId in the header carrier to n/a" in new FieldTransformerScope {
      transformer.sessionIdFrom(hc.copy(sessionId = None)) shouldBe "n/a"
    }

    "transform user agent in the request headers to user agent" in new FieldTransformerScope {
      transformer.userAgentOf(request) shouldBe userAgent
    }

    "transform no user agent in the request headers to n/a" in new FieldTransformerScope {
      transformer.userAgentOf(FakeRequest()) shouldBe "n/a"
    }

    "transform non authorised user to UserTaxIdentifiers containing no identifiers" in new FieldTransformerScope {
      transformer.userTaxIdentifiersFromEnrolments(None) shouldBe expectedUserTaxIdentifiers()
    }

    "transform paye authorised user to UserTaxIdentifiers containing one identifier i.e. the SH233544B" in new FieldTransformerScope {
      transformer.userTaxIdentifiersFromEnrolments(Some(payeUser)) shouldBe expectedUserTaxIdentifiers(
        nino = Some("SH233544B")
      )
    }

    "transform business tax authorised user to UserTaxIdentifiers containing all the Business Tax Identifiers (and HMCE-VATDEC-ORG endorsement)" in new FieldTransformerScope {
      transformer.userTaxIdentifiersFromEnrolments(Some(bizTaxUserWithVatDec)) shouldBe expectedUserTaxIdentifiers(
        utr = Some("sa"),
        ctUtr = Some("ct"),
        vrn = Some("vrn1"),
        empRef = Some(EmpRef("officeNum", "officeRef").value)
      )
    }

    "transform business tax authorised user to UserTaxIdentifiers containing all the Business Tax Identifiers (and HMCE-VATVAR-ORG endorsement)" in new FieldTransformerScope {
      transformer.userTaxIdentifiersFromEnrolments(Some(bizTaxUserWithVatVar)) shouldBe expectedUserTaxIdentifiers(
        utr = Some("sa"),
        ctUtr = Some("ct"),
        vrn = Some("vrn2"),
        empRef = Some(EmpRef("officeNum", "officeRef").value)
      )
    }
  }

}

class FieldTransformerScope {
  lazy val transformer = new FieldTransformer {}

  lazy val userId   = UserId("456")
  lazy val payeUser =
    Enrolments(
      Set(
        Enrolment("HMRC-NI").withIdentifier("NINO", "SH233544B")
      )
    )

  lazy val bizTaxUserWithVatDec =
    Enrolments(
      Set(
        Enrolment("IR-SA").withIdentifier("UTR", "sa"),
        Enrolment("IR-CT").withIdentifier("UTR", "ct"),
        Enrolment("HMCE-VATDEC-ORG").withIdentifier("VATRegNo", "vrn1"),
        Enrolment("IR-PAYE")
          .withIdentifier("TaxOfficeNumber", "officeNum")
          .withIdentifier("TaxOfficeReference", "officeRef")
      )
    )

  lazy val bizTaxUserWithVatVar =
    Enrolments(
      Set(
        Enrolment("IR-SA").withIdentifier("UTR", "sa"),
        Enrolment("IR-CT").withIdentifier("UTR", "ct"),
        Enrolment("HMCE-VATVAR-ORG").withIdentifier("VATRegNo", "vrn2"),
        Enrolment("IR-PAYE")
          .withIdentifier("TaxOfficeNumber", "officeNum")
          .withIdentifier("TaxOfficeReference", "officeRef")
      )
    )

  val sessionId: String              = "sessionIdValue"
  val hc                             = HeaderCarrier(userId = Some(userId), sessionId = Some(SessionId(sessionId)))
  val userAgent: String              = "Mozilla"
  val name: String                   = "name"
  val email: String                  = "email"
  val subject: String                = "subject"
  val message: String                = "message"
  val referrer: String               = "referrer"
  lazy val request                   = FakeRequest().withHeaders(("User-Agent", userAgent))
  lazy val requestAuthenticatedByIda =
    FakeRequest().withHeaders(("User-Agent", userAgent)).withSession(("ap", "IDA"), (SessionKeys.authToken, "12345"))

  def expectedUserTaxIdentifiers(
    nino: Option[String] = None,
    ctUtr: Option[String] = None,
    utr: Option[String] = None,
    vrn: Option[String] = None,
    empRef: Option[String] = None
  ) = UserTaxIdentifiers(nino, ctUtr, utr, vrn, empRef)
}

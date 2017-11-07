package connectors.deskpro.domain

import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys, UserId}
import uk.gov.hmrc.play.test.UnitSpec

class FieldTransformerSpec extends UnitSpec with OneAppPerSuite {

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

    "transform userId in the header carrier to user id" in new FieldTransformerScope {
      transformer.userIdFrom(request, hc) shouldBe userId.value
    }

    "transform no userId in the header carrier to n/a" in new FieldTransformerScope {
      transformer.userIdFrom(request, hc.copy(userId = None)) shouldBe "n/a"
    }

    "transforms userId in the header carrier to user id" in new FieldTransformerScope {
      transformer.userIdFrom(requestAuthenticatedByIda, hc) shouldBe userId.value
    }

    "transforms userId in the header carrier to n/a if the suppressUserIdInSupportRequests session key is set to true" in new FieldTransformerScope {
      val requestWithSuppressedUserId = FakeRequest().withHeaders(("User-Agent", userAgent)).withSession(SessionKeys.authProvider -> "IDA", SessionKeys.sensitiveUserId -> "true")
      transformer.userIdFrom(requestWithSuppressedUserId, hc) shouldBe "n/a"
    }

    "transforms no userId in the header carrier to n/a" in new FieldTransformerScope {
      transformer.userIdFrom(FakeRequest(), hc.copy(userId = None)) shouldBe "n/a"
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
      transformer.userTaxIdentifiersFromEnrolments(Some(payeUser)) shouldBe expectedUserTaxIdentifiers(nino = Some("SH233544B"))
    }

    "transform business tax authorised user to UserTaxIdentifiers containing all the Business Tax Identifiers (and HMCE-VATDEC-ORG endorsement)" in new FieldTransformerScope {
      transformer.userTaxIdentifiersFromEnrolments(Some(bizTaxUserWithVatDec)) shouldBe expectedUserTaxIdentifiers(utr = Some("sa"), ctUtr = Some("ct"), vrn = Some("vrn1"), empRef = Some(EmpRef("officeNum", "officeRef").value))
    }

    "transform business tax authorised user to UserTaxIdentifiers containing all the Business Tax Identifiers (and HMCE-VATVAR-ORG endorsement)" in new FieldTransformerScope {
      transformer.userTaxIdentifiersFromEnrolments(Some(bizTaxUserWithVatVar)) shouldBe expectedUserTaxIdentifiers(utr = Some("sa"), ctUtr = Some("ct"), vrn = Some("vrn2"), empRef = Some(EmpRef("officeNum", "officeRef").value))
    }
  }

}

class FieldTransformerScope {
  val transformer = new FieldTransformer {}


  val userId = UserId("456")
  val payeUser =
    Enrolments(Set(
      Enrolment("HMRC-NI").withIdentifier("NINO", "SH233544B")
    ))

  val bizTaxUserWithVatDec =
    Enrolments(Set(
      Enrolment("IR-SA").withIdentifier("UTR", "sa"),
      Enrolment("IR-CT").withIdentifier("UTR", "ct"),
      Enrolment("HMCE-VATDEC-ORG").withIdentifier("VATRegNo", "vrn1"),
      Enrolment("IR-PAYE").withIdentifier("TaxOfficeNumber", "officeNum").withIdentifier("TaxOfficeReference", "officeRef")
    ))

  val bizTaxUserWithVatVar =
    Enrolments(Set(
      Enrolment("IR-SA").withIdentifier("UTR", "sa"),
      Enrolment("IR-CT").withIdentifier("UTR", "ct"),
      Enrolment("HMCE-VATVAR-ORG").withIdentifier("VATRegNo", "vrn2"),
      Enrolment("IR-PAYE").withIdentifier("TaxOfficeNumber", "officeNum").withIdentifier("TaxOfficeReference", "officeRef")
    ))

  val sessionId: String = "sessionIdValue"
  val hc = HeaderCarrier(userId = Some(userId), sessionId = Some(SessionId(sessionId)))
  val userAgent: String = "Mozilla"
  val name: String = "name"
  val email: String = "email"
  val subject: String = "subject"
  val message: String = "message"
  val referrer: String = "referer"
  val request = FakeRequest().withHeaders(("User-Agent", userAgent))
  val requestAuthenticatedByIda = FakeRequest().withHeaders(("User-Agent", userAgent)).withSession((SessionKeys.authProvider, "IDA"))

  def expectedUserTaxIdentifiers(nino: Option[String] = None,
                                 ctUtr: Option[String] = None,
                                 utr: Option[String] = None,
                                 vrn: Option[String] = None,
                                 empRef: Option[String] = None) = UserTaxIdentifiers(nino, ctUtr, utr, vrn, empRef)
}


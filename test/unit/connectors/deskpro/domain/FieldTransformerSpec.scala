package connectors.deskpro.domain

import play.api.test.FakeRequest
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.audit.http.{HeaderCarrier, UserId}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProviderIds, AuthContext}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class FieldTransformerSpec extends UnitSpec with WithFakeApplication {

  "Field Transformer" should {

    "transform javascript not enabled" in new FieldTransformerScope {
      transformer.ynValueOf(javascript = false) shouldBe "N"
    }

    "transform javascript  enabled" in new FieldTransformerScope {
      transformer.ynValueOf(javascript = true) shouldBe "Y"
    }

    "transform session authenticated by Ida to paye" in new FieldTransformerScope {
      transformer.areaOfTaxOf(requestAuthenticatedByIda) shouldBe "paye"
    }

    "transforms session authenticated by GGW to biztax" in new FieldTransformerScope {
      transformer.areaOfTaxOf(requestAuthenticatedByGG) shouldBe "biztax"
    }

    "transforms session authenticated by NTC to taxcreditrenewals" in new FieldTransformerScope {
      transformer.areaOfTaxOf(requestAuthenticatedByNTC) shouldBe "taxcreditrenewals"
    }

    "transforms no user to an unknown area of tax" in new FieldTransformerScope {
      transformer.areaOfTaxOf(request) shouldBe "n/a"
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
      transformer.userTaxIdentifiersOf(None) shouldBe expectedUserTaxIdentifiers()
    }

    "transform paye authorised user to UserTaxIdentifiers containing one identifier i.e. the SH233544B" in new FieldTransformerScope {
      transformer.userTaxIdentifiersOf(Some(payeUser)) shouldBe expectedUserTaxIdentifiers(nino = Some(Nino("SH233544B")))
    }

    "transform business tax authorised user to UserTaxIdentifiers containing all the Business Tax Identifiers" in new FieldTransformerScope {
      transformer.userTaxIdentifiersOf(Some(bizTaxUser)) shouldBe expectedUserTaxIdentifiers(utr = Some(SaUtr("sa")), ctUtr = Some(CtUtr("ct")), vrn = Some(Vrn("vrn")), empRef = Some(EmpRef("officeNum", "officeRef")))
    }
  }

}

class FieldTransformerScope {
  val transformer = new FieldTransformer {}

  val vatAccount = Some(VatAccount("/vatRoot", Vrn("vrn")))
  val saAccount = Some(SaAccount("/saRoot", SaUtr("sa")))
  val ctAccount = Some(CtAccount("/ctRoot", CtUtr("ct")))
  val epayeAccount = Some(EpayeAccount("epayeRoot", EmpRef("officeNum", "officeRef")))
  
  val userId = UserId("456")
  val payeUser = AuthContext(Authority(s"/auth/oid/$userId",  Accounts(Some(PayeAccount("payeRoot", Nino("SH233544B")))), None, None))
  val bizTaxUser = AuthContext(Authority(s"/auth/oid/$userId",  Accounts(sa = saAccount, ct = ctAccount, vat = vatAccount, epaye = epayeAccount), None, None))

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
  val requestAuthenticatedByGG = FakeRequest().withHeaders(("User-Agent", userAgent)).withSession((SessionKeys.authProvider, AuthenticationProviderIds.GovernmentGatewayId))
  val requestAuthenticatedByNTC = FakeRequest().withHeaders(("User-Agent", userAgent)).withSession((SessionKeys.authProvider, "NTC"))

  def expectedUserTaxIdentifiers(nino: Option[Nino] = None,
                                 ctUtr: Option[CtUtr] = None,
                                 utr: Option[SaUtr] = None,
                                 vrn: Option[Vrn] = None,
                                 empRef: Option[EmpRef] = None) = UserTaxIdentifiers(nino, ctUtr, utr, vrn, empRef)
}


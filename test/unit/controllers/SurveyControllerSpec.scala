package controllers

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class SurveyControllerSpec extends UnitSpec with WithFakeApplication {

  "ticketId regex" should {
    "validates correct ticket refs" in new SurveyControllerApplication {
     controller.validateTicketId("HMRC-Z2V6DUK5") shouldBe(true)
     controller.validateTicketId("HMRC-Z2A6DA5") shouldBe(true)
     controller.validateTicketId("HMRC-Z2A6") shouldBe(true)
     controller.validateTicketId("HMRC-Z") shouldBe(true)
    }

    "rejects invalid ticket refs" in new SurveyControllerApplication {
      controller.validateTicketId("HMRC-") shouldBe(false)
      controller.validateTicketId("") shouldBe(false)
      controller.validateTicketId("HMRC-Z2A6Z2A6A") shouldBe(false)
      controller.validateTicketId("HMRC-Z2V6dUK5") shouldBe(false)
      controller.validateTicketId("HMRC-Z2'6DUK5") shouldBe(false)
      controller.validateTicketId("HMRC-Z2!6DUK5") shouldBe(false)
      controller.validateTicketId("HMRC-Z2 6DUK5") shouldBe(false)
    }
  }

}

class SurveyControllerApplication extends MockitoSugar {
  val controller = new SurveyController {
    override lazy val auditConnector = mock[AuditConnector]
    override lazy val authConnector = mock[AuthConnector]
  }
}

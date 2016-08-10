package controllers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class SurveyControllerSpec extends UnitSpec with WithFakeApplication {

  "ticketId regex" should {
    "validates correct ticket refs" in new SurveyControllerApplication {
     controller.validateTicketId("HMRC-Z2V6DUK5") shouldBe true
     controller.validateTicketId("HMRC-Z2A6DA5") shouldBe true
     controller.validateTicketId("HMRC-Z2A6") shouldBe true
     controller.validateTicketId("HMRC-Z") shouldBe true
    }

    "rejects invalid ticket refs" in new SurveyControllerApplication {
      controller.validateTicketId("HMRC-") shouldBe false
      controller.validateTicketId("") shouldBe false
      controller.validateTicketId("HMRC-Z2A6Z2A6A") shouldBe false
      controller.validateTicketId("HMRC-Z2V6dUK5") shouldBe false
      controller.validateTicketId("HMRC-Z2'6DUK5") shouldBe false
      controller.validateTicketId("HMRC-Z2!6DUK5") shouldBe false
      controller.validateTicketId("HMRC-Z2 6DUK5") shouldBe false
    }
  }

  "SurveyController" should {
    "produce an audit result for a valid form" in new SurveyControllerApplication {
      implicit val request = FakeRequest("GET", "/blah", FakeHeaders(), Map[String, Seq[String]](
        "helpful" -> Seq("2"),
        "ticket-id" -> Seq("HMRC-Z"),
        "service-id" -> Seq("abcdefg")
      ))

      val audit = controller.getAuditEventOrFormErrors
      val dataFuture = audit flatMap { case Left(Some(future)) => future }
      val data = Await.result(dataFuture, 5.seconds)

      data.auditSource shouldBe "frontend"
      data.auditType shouldBe "DeskproSurvey"
      data.detail shouldBe Map[String, String](
        "helpful" -> "2",
        "speed" -> "0",
        "improve" -> "",
        "ticketId" -> "HMRC-Z",
        "serviceId" -> "abcdefg"
      )
    }

    "product errors for an invalid form" in new SurveyControllerApplication {
      implicit val request = FakeRequest("GET", "/blah", FakeHeaders(), Map[String, Seq[String]](
        "helpful" -> Seq("2"),
        "ticket-id" -> Seq("HMRC-Z"),
        "service-id" -> Seq("abcdefg122445345345dfddfds234234sdf")
      ))

      val audit = controller.getAuditEventOrFormErrors
      val errorsFuture = audit flatMap { case Right(errorSeq) => errorSeq }
      val errors = Await.result(errorsFuture, 5.seconds)

      errors.size shouldBe 1
      errors.headOption map { _.key } shouldBe Some("service-id")
    }
  }

}



class SurveyControllerApplication extends MockitoSugar {
  val controller = new SurveyController {
    override lazy val auditConnector = mock[AuditConnector]
    override lazy val authConnector = mock[AuthConnector]
  }
}

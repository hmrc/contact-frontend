package controllers

import config.CFConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class SurveyControllerSpec extends UnitSpec {

  "ticketId regex" should {
    "validates correct ticket refs" in new SurveyControllerApplication {
     controller.validateTicketId("GFBN-8051-KLNY") shouldBe true
    }

    "rejects invalid ticket refs" in new SurveyControllerApplication {
      controller.validateTicketId("GFBN-8051-") shouldBe false
      controller.validateTicketId("") shouldBe false
    }
  }

  "SurveyController" should {
    "produce an audit result for a valid form" in new SurveyControllerApplication {
      implicit val request = FakeRequest("GET", "/blah", FakeHeaders(), Map[String, Seq[String]](
        "helpful" -> Seq("2"),
        "ticket-id" -> Seq("GFBN-8051-KLNY"),
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
        "ticketId" -> "GFBN-8051-KLNY",
        "serviceId" -> "abcdefg"
      )
    }

    "product errors for an invalid form" in new SurveyControllerApplication {
      implicit val request = FakeRequest("GET", "/blah", FakeHeaders(), Map[String, Seq[String]](
        "helpful" -> Seq("2"),
        "ticket-id" -> Seq("GFBN-8051-KLNY"),
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
  val controller = new SurveyController( mock[AuditConnector])(mock[MessagesApi], new CFConfig(Environment.simple(), Configuration()))
}

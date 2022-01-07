/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.enrolments

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{Json, _}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

class EnrolmentsConnectorSpec extends AnyWordSpec with Matchers {

  private def authConnectorReturning(json: JsValue): AuthConnector =
    new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
        hc: HeaderCarrier,
        ec: ExecutionContext
      ): Future[A] =
        Future.successful(json.as[A](retrieval.reads))
    }

  "Given a request with an auth token from session, retrieving enrolments" should {

    "return some enrolments if returned from auth connector" in {
      val json                         = Json.obj("allEnrolments" -> Seq(Json.obj("key" -> "enrolmentKey")))
      val authConnector: AuthConnector = authConnectorReturning(json)
      val enrolmentsConnector          = EnrolmentsConnector(authConnector)

      val request: Request[_] =
        FakeRequest().withSession((SessionKeys.authToken, "some-auth-token"))
      val hc: HeaderCarrier   = HeaderCarrier()

      val maybeEnrolments = Await.result(
        enrolmentsConnector.maybeAuthenticatedUserEnrolments()(hc, request),
        Duration(1, SECONDS)
      )
      maybeEnrolments shouldBe Some(Enrolments(Set(Enrolment("enrolmentKey"))))
    }

    "return empty set if empty set returned from auth connector" in {
      val json                         = Json.obj("allEnrolments" -> Seq[JsValue]())
      val authConnector: AuthConnector = authConnectorReturning(json)
      val enrolmentsConnector          = EnrolmentsConnector(authConnector)

      val request: Request[_] =
        FakeRequest().withSession((SessionKeys.authToken, "some-auth-token"))
      val hc: HeaderCarrier   = HeaderCarrier()

      val maybeEnrolments = Await.result(
        enrolmentsConnector.maybeAuthenticatedUserEnrolments()(hc, request),
        Duration(1, SECONDS)
      )
      maybeEnrolments shouldBe Some(Enrolments(Set.empty[Enrolment]))
    }

    "return None if connection to auth fails" in {
      val authConnector       = new AuthConnector {
        override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
          hc: HeaderCarrier,
          ec: ExecutionContext
        ): Future[A] =
          Future.failed(new Exception("This is an expected exception"))
      }
      val enrolmentsConnector = EnrolmentsConnector(authConnector)

      val request: Request[_] =
        FakeRequest().withSession((SessionKeys.authToken, "some-auth-token"))
      val hc: HeaderCarrier   = HeaderCarrier()

      val maybeEnrolments = Await.result(
        enrolmentsConnector.maybeAuthenticatedUserEnrolments()(hc, request),
        Duration(1, SECONDS)
      )
      maybeEnrolments shouldBe None
    }
  }

  "Given a request without an auth token, retrieving enrolments" should {
    "return none" in {
      val json                         = Json.obj("allEnrolments" -> Seq(Json.obj("key" -> "enrolmentKey")))
      val authConnector: AuthConnector = authConnectorReturning(json)
      val enrolmentsConnector          = EnrolmentsConnector(authConnector)

      val request: Request[_] = FakeRequest()
      val hc: HeaderCarrier   = HeaderCarrier()

      val maybeEnrolments = Await.result(
        enrolmentsConnector.maybeAuthenticatedUserEnrolments()(hc, request),
        Duration(1, SECONDS)
      )
      maybeEnrolments shouldBe None
    }
  }
}

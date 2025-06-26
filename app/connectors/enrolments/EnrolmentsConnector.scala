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

package connectors.enrolments

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class EnrolmentsConnector @Inject() (authConnector: AuthConnector)(using ExecutionContext)
    extends AuthorisedFunctions with Logging {

  def maybeAuthenticatedUserEnrolments()(using request: Request[?])(using HeaderCarrier): Future[Option[Enrolments]] =
    if (request.session.get(SessionKeys.authToken).isDefined) {
      authorised()
        .retrieve(Retrievals.allEnrolments) { enrolments =>
          Future.successful(Some(enrolments))
        }
        .recover {
          case NonFatal(_) =>
            logger.error("Session has an authToken, but retrieval of enrolments failed")
            None
        }
    } else {
      Future.successful(None)
    }

}

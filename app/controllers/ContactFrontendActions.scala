package controllers

import play.api.mvc.Request
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait ContactFrontendActions extends AuthorisedFunctions {

  protected def maybeAuthenticatedUserEnrolments()(
    implicit hc: HeaderCarrier,
    request: Request[_]): Future[Option[Enrolments]] =
    if (request.session.get(SessionKeys.authToken).isDefined) {
      authorised()
        .retrieve(Retrievals.allEnrolments) { enrolments =>
          Future.successful(Some(enrolments))
        }
        .recover { case _ => None }
    } else {
      Future.successful(None)
    }

}

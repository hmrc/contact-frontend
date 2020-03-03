package controllers

import play.api.mvc.Request
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

trait ContactFrontendActions extends AuthorisedFunctions {

  implicit val executionContext: ExecutionContext

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

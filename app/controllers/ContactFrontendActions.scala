package controllers

import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }

trait ContactFrontendActions extends Actions {

  protected def maybeAuthenticatedUserAccounts()(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[Accounts]] = {
    if (request.session.get(SessionKeys.authToken).isDefined) {
      authConnector.currentAuthority.map(
        authorityOption => authorityOption.map(_.accounts)
      ).recover {
        case _: Throwable => None
      }
    } else {
      Future.successful(None)
    }
  }

}

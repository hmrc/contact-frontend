package controllers

import play.api.mvc.Request
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait ContactFrontendActions extends Actions {

  protected def maybeAuthenticatedUserAccounts()(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[Accounts]] = {
    if (request.session.get(SessionKeys.authToken).isDefined) {
      authConnector.currentAuthority.map(authorityOption => authorityOption.map(_.accounts))
    } else {
      Future.successful(None)
    }
  }

}

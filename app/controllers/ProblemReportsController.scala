package controllers

import controllers.common.BaseController
import controllers.common.actions.Actions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.validators.Validators._
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.play.microservice.UnauthorizedException
import uk.gov.hmrc.play.microservice.auth.AuthConnector
import uk.gov.hmrc.play.microservice.auth.domain.Accounts
import uk.gov.hmrc.play.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future

trait ProblemReportsController extends BaseController with Actions {

  lazy val auditConnector: AuditConnector = ???
  lazy val hmrcDeskproConnector: HmrcDeskproConnector = ???
  lazy val authConnector: AuthConnector = ???

  val form = Form[ProblemReport](
    mapping(
      "report-name" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.problem_report.name_too_long", name => name.size <= 70)
        .verifying("error.common.problem_report.name_invalid_characters", name => name.matches( """^[A-Za-z\-.,()'"\s]+$""")),
      "report-email" -> emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),
      "report-action" -> text
        .verifying("error.common.problem_report.action_mandatory", action => !action.isEmpty)
        .verifying("error.common.comments_too_long", action => action.size <= 1000),
      "report-error" -> text
        .verifying("error.common.problem_report.action_mandatory", error => !error.isEmpty)
        .verifying("error.common.comments_too_long", error => error.size <= 1000),
      "isJavascript" -> boolean
    )(ProblemReport.apply)(ProblemReport.unapply)
  )

  def report = Action.async(doReport()(_))

  def doReport(thankYouMessage: Option[String] = None, accounts: Option[Accounts] = None)(implicit request: Request[AnyRef]) = {
    form.bindFromRequest.fold(
      error => {
        if (!error.data.getOrElse("isJavascript", "true").toBoolean) {
          Future.successful(Ok(views.html.support.problem_reports_error_nonjavascript(referrerFrom(request))))
        } else {
          Future.successful(BadRequest(Json.toJson(Map("status" -> "ERROR"))))
        }
      },
      problemReport => {
        for {
          maybeUserAccounts <- accounts.fold(ifEmpty = maybeAuthenticatedUserAccounts)(_ => Future.successful(accounts))
          ticketIdOption <- createTicket(problemReport, request, maybeUserAccounts)
        }
        yield {
          val ticketId: String = ticketIdOption.map(_.ticket_id.toString).getOrElse("Unknown")
          if (!problemReport.isJavascript) Ok(views.html.support.problem_reports_confirmation_nonjavascript(ticketId, thankYouMessage))
          else Ok(Json.toJson(Map("status" -> "OK", "message" -> views.html.support.ticket_created_body(ticketId, thankYouMessage).toString())))
        }
      })
  }

  private def maybeAuthenticatedUserAccounts()(implicit request: Request[AnyRef]): Future[Option[Accounts]] = {
    if (request.session.get(SessionKeys.authToken).isDefined) {
      authConnector.currentAuthority.map(authorityOption => authorityOption.map(_.accounts)).recover {
        case _: UnauthorizedException => {
          Logger.info(s"unauthorized from auth, expected for TCR")
          None
        }
      }
    } else {
      Future.successful(None)
    }
  }

  private def createTicket(problemReport: ProblemReport, request: Request[AnyRef], accountsOption: Option[Accounts]) = {
    implicit val hc = HeaderCarrier.fromSessionAndHeaders(request.session, request.headers)
    hmrcDeskproConnector.createDeskProTicket(
      problemReport.reportName,
      problemReport.reportEmail,
      "Support Request",
      problemMessage(problemReport.reportAction, problemReport.reportError),
      referrerFrom(request),
      problemReport.isJavascript,
      request,
      accountsOption
    )
  }

  private[controllers] def problemMessage(action: String, error: String): String = {
    s"""
    ${Messages("problem_report.action")}:
    $action

    ${Messages("problem_report.error")}:
    $error
    """
  }

  private def referrerFrom(request: Request[AnyRef]): String = {
    request.headers.get("referer").getOrElse("/home")
  }
}

case class ProblemReport(reportName: String, reportEmail: String, reportAction: String, reportError: String, isJavascript: Boolean)


object ProblemReportsController extends ProblemReportsController {
  override lazy val auditConnector: AuditConnector = AuditConnector
  override lazy val hmrcDeskproConnector: HmrcDeskproConnector = HmrcDeskproConnector
  override lazy val authConnector: AuthConnector = AuthConnector
}
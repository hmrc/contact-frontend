package controllers

import config.FrontendAuthConnector
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future
import scala.util.Try

trait SurveyController
  extends FrontendController
    with Actions {

  def auditConnector: AuditConnector

  def survey(ticketId: String) = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.survey(ticketId))
    )
  }

  def submit() = UnauthorisedAction.async { implicit request =>
    Future.successful(submitSurveyAction)
  }

  def confirmation() = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.survey_confirmation())
    )
  }

  def failure() = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.survey_failure())
    )
  }

  private[controllers] def submitSurveyAction(implicit request: Request[_]): Result = {
    Try {
      surveyForm.bindFromRequest().value.map {
        data => buildAuditEvent(data).map {
          auditEvent => auditConnector.sendEvent(auditEvent)
        }
      }
    }
    Redirect(routes.SurveyController.confirmation())
  }

  private[controllers] def buildAuditEvent(formData: SurveyFormData)(implicit request: Request[_], hc: HeaderCarrier): Future[DataEvent] = {
    val auditDetail = questionnaireFormDataToMap(formData)
    Future.successful(DataEvent(auditSource = "frontend", auditType = "DeskproSurvey", tags = hc.headers.toMap, detail = auditDetail))
  }

  private def questionnaireFormDataToMap(formData: SurveyFormData): Map[String, String] = {
    formData.getClass.getDeclaredFields.map {
      field =>
        field.setAccessible(true)
        field.getName -> (field.get(formData) match {
          case Some(x) => x.toString
          case xs: Seq[Any] => xs.mkString(",")
          case x => x.toString
        })
    }.toMap
  }

  object FormFields {
    val helpful = "helpful"
    val speed = "speed"
    val improve = "improve"
    val ticketId = "ticket-id"
  }

  private val ratingScale = optional(number(min = 1, max = 5, strict = false))

  private[controllers] def surveyForm = Form[SurveyFormData](
    mapping(
      FormFields.helpful -> ratingScale,
      FormFields.speed -> ratingScale,
      FormFields.improve -> optional(text(maxLength = 2500)),
      FormFields.ticketId -> optional(text(maxLength = 10))
    )(SurveyFormData.apply)(SurveyFormData.unapply)
  )
}

case class SurveyFormData(
                           helpful: Option[Int],
                           speed: Option[Int],
                           improve: Option[String],
                           ticketId: Option[String]
                         )


object SurveyController extends SurveyController {
  override val authConnector = FrontendAuthConnector
  override val auditConnector = YTAAuditConnector
}

object YTAAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}


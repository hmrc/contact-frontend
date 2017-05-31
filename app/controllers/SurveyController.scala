package controllers

import config.FrontendAuthConnector
import play.api.data.{Form, FormError}
import play.api.Logger
import play.api.data.Forms._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.http.HeaderCarrier

import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.util.matching.Regex
import scala.concurrent.Future
import scala.util.Try


trait SurveyController
  extends FrontendController
    with Actions {

  val TICKET_ID_REGEX      = new Regex("^HMRC-([A-Z0-9]|#){1,8}+$")
  val TICKET_ID_MAX_LENGTH = 5+8

  def validateTicketId(ticketId:String) = TICKET_ID_REGEX.findFirstIn(ticketId).isDefined

  def auditConnector: AuditConnector

  def survey(ticketId: String, serviceId: String) = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.survey(ticketId, serviceId))
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

  private[controllers] def getAuditEventOrFormErrors(implicit request: Request[_]): Either[Option[Future[DataEvent]], Seq[FormError]] = {
    val form = surveyForm.bindFromRequest()
    form.errors match {
      case Nil => Left(Try(form.value.map(buildAuditEvent)).toOption.flatten)
      case e @ _ => Right(e)
    }
  }

  private[controllers] def submitSurveyAction(implicit request: Request[_]): Result = {
    getAuditEventOrFormErrors match {
      case Left(eventOption) => eventOption foreach { dataEventFuture => dataEventFuture.foreach(auditConnector.sendEvent) }
      case Right(errors) => errors foreach { error => Logger.error(s"Error processing survey form field: '${error.key}' message='${error.message}' args='${error.args.mkString(",")}'") }
    }
    Redirect(routes.SurveyController.confirmation())
  }

  private[controllers] def buildAuditEvent(formData: SurveyFormData)(implicit request: Request[_], hc: HeaderCarrier): Future[DataEvent] = {
    Future.successful(DataEvent(auditSource = "frontend", auditType = "DeskproSurvey", tags = hc.headers.toMap, detail = formData.toStringMap))
  }

  private val ratingScale = optional(number(min = 1, max = 5, strict = false))

  private[controllers] def surveyForm = Form[SurveyFormData](
    mapping(
      SurveyFormFields.helpful -> ratingScale,
      SurveyFormFields.speed -> ratingScale,
      SurveyFormFields.improve -> optional(text(maxLength = 2500)),
      SurveyFormFields.ticketId -> optional(text(maxLength = TICKET_ID_MAX_LENGTH)).verifying(ticketId => validateTicketId(ticketId.getOrElse(""))),
      SurveyFormFields.serviceId -> optional(text(maxLength = 20)).verifying(serviceId => serviceId.getOrElse("").length>0)
    )(SurveyFormData.apply)(SurveyFormData.unapply)
  )
}


object SurveyFormFields {
  val helpful = "helpful"
  val speed = "speed"
  val improve = "improve"
  val ticketId = "ticket-id"
  val serviceId = "service-id"
}


case class SurveyFormData(helpful: Option[Int],
                          speed: Option[Int],
                          improve: Option[String],
                          ticketId: Option[String],
                          serviceId: Option[String]) {
  def toStringMap: Map[String, String] = collection.immutable.HashMap(
    "helpful" -> helpful.getOrElse(0).toString,
    "speed" -> speed.getOrElse(0).toString,
    "improve" -> improve.getOrElse(""),
    "ticketId" -> ticketId.getOrElse(""),
    "serviceId" -> serviceId.getOrElse("")
  )
}


object SurveyController extends SurveyController {
  override val authConnector = FrontendAuthConnector
  override val auditConnector = YTAAuditConnector
}


object YTAAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

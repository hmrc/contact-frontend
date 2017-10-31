package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}

import scala.concurrent.Future
import scala.util.Try
import scala.util.matching.Regex

@Singleton
class SurveyController @Inject() (auditConnector : AuditConnector)(implicit val messagesApi : MessagesApi, val authConnector : AuthConnector, appConfig : AppConfig)
  extends FrontendController
    with Actions with I18nSupport {

  val TICKET_ID_REGEX      = new Regex("^HMRC-([A-Z0-9]|#){1,8}+$")
  val TICKET_ID_MAX_LENGTH = 5+8

  def validateTicketId(ticketId:String) = TICKET_ID_REGEX.findFirstIn(ticketId).isDefined

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

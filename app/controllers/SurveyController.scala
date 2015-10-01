package controllers

import config.FrontendAuthConnector
import connectors.deskpro.HmrcDeskproConnector
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Request, Result}
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import uk.gov.hmrc.play.validators.Validators

import scala.concurrent.Future

trait SurveyController
  extends FrontendController
  with Actions {


  def unauthenticatedSurveyForm() = UnauthorisedAction.async { implicit request =>
    Future.successful(
      Ok(views.html.survey())
    )
  }


}

object SurveyController extends SurveyController {
  override val authConnector = FrontendAuthConnector
}

package controllers

import play.api.mvc._
import uk.gov.hmrc.play.config.RunMode

class ApplicationController extends Controller with RunMode {

  def options(path: String) = Action {
    implicit request =>
      env match {
        case "Dev" => Ok.withHeaders(
          "Access-Control-Allow-Origin" -> "*",
          "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
          "Access-Control-Allow-Headers" -> request.headers.get("Access-Control-Request-Headers").getOrElse(""),
          "Access-Control-Expose-Headers" -> "Location",
          "Access-Control-Allow-Credentials" -> "true",
          "Access-Control-Max-Age" -> (60 * 60 * 24).toString
        )
        case _ => Forbidden
    }

  }
}
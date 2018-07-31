package controllers

import javax.inject.Inject

import play.api.{Configuration, Environment}
import play.api.mvc._
import uk.gov.hmrc.play.config.RunMode

class ApplicationController @Inject() (environment : Environment, configuration : Configuration) extends Controller with RunMode {

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  def allowedOrigins = configuration.getString("corsfilter.allowedOrigins")

  def options(path: String) = Action {
    implicit request => Ok.withHeaders(
      "Access-Control-Allow-Origin" -> "https://ewf.companieshouse.gov.uk",
      "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers" -> "User-Agent,X-Requested-With,Cache-Control,Connection,Accept-Encoding,Origin,Referer,Csrf-Token",
      "Access-Control-Allow-Credentials" -> "false"
    )
//      env match {
//        case "Dev" => Ok.withHeaders(
//          "Access-Control-Allow-Origin" -> "*",
//          "Access-Control-Allow-Headers" -> request.headers.get("Access-Control-Request-Headers").getOrElse(""),
//          "Access-Control-Expose-Headers" -> "Location",
//          "Access-Control-Allow-Credentials" -> "true",
//          "Access-Control-Max-Age" -> (60 * 60 * 24).toString
//        )
//        case _ => Forbidden
//    }
  }
}
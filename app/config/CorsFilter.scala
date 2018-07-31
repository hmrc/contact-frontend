package config

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CorsFilter @Inject() (environment : Environment, configuration : Configuration)(implicit override val mat: Materializer)
  extends RunMode with Filter {

  override protected def mode = environment.mode

  override protected def runModeConfiguration = configuration

  val enableAnyOrigin: Boolean = env == "Dev"

  def apply(nextFilter: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    if(enableAnyOrigin) {
      nextFilter(requestHeader).map { result =>
        result.withHeaders("Access-Control-Allow-Origin" -> "*", "Access-Control-Expose-Headers" -> "Location")
      }
    } else {
        nextFilter(requestHeader)
    }
  }

}

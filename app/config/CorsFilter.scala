package config

import play.api.mvc.{Result, RequestHeader, Filter}
import uk.gov.hmrc.play.config.RunMode

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object CorsFilter extends CorsFilter with RunMode {
  override val enableAnyOrigin: Boolean = env == "Dev"
}

trait CorsFilter extends Filter {

  def enableAnyOrigin: Boolean

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

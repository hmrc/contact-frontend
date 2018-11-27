package views.helpers

import play.api.data.Form

object ServiceParameter {

  def extractServiceParameter[T](feedbackForm: Form[T], service: Option[String]) =
    feedbackForm("service").value match {
      case Some("unknown") => service.getOrElse("unknown")
      case Some(service) => service
      case None => "unknown"
    }

}

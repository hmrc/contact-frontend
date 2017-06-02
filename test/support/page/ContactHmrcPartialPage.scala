package support.page

import support.steps.Env

class ContactHmrcPartialPage(submitUrl: String, 
                             service: Option[String] = None, 
                             renderFormOnly: Option[Boolean] = None) extends ContactHmrcPage {

  override val url = {
    val queryParameters = List(
      s"submitUrl=$submitUrl",
      "csrfToken=token",
      service.fold("")(service => s"service=$service"),
      renderFormOnly.fold("")(formOnly => s"renderFormOnly=$formOnly")
    ).filter(_.trim.nonEmpty).mkString("&")

    Env.host + s"/contact/contact-hmrc/form?$queryParameters"
  }

}
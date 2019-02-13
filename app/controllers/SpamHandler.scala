package controllers

import play.api.Logger
import play.api.mvc.{Result, Results}

import scala.concurrent.Future

object SpamHandler {
  def ignoreIfSpam(contactForm: ContactForm)(block: => Future[Result]): Future[Result] = {
    def hasIdeographicCharacters(s: String): Boolean =
      s.exists(c => Character.isIdeographic(c))

    if (hasIdeographicCharacters(contactForm.contactComments)) {
      import contactForm._
      Logger.warn(s"Rejecting spammer's form submission, additional info: " +
        s"[email: $contactEmail, message: $contactComments, referrer: $referer, service: $service]")
      // returning 200 and no useful info on purpose for now
      Future.successful(Results.Ok("Error submitting form. Apologies for any inconvenience caused."))
    } else {
      block
    }
  }
}

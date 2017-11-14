package util

import org.apache.commons.validator.routines.EmailValidator

case class DeskproEmailValidator() {

  private val validator = EmailValidator.getInstance(false)

  def validate(email: String): Boolean = validator.isValid(email)
}

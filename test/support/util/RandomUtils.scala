package uk.gov.hmrc.integration.util

import scala.util.Random

object RandomUtils {

  def randString(howManyChars: Integer): String = {
    Random.alphanumeric take howManyChars mkString ""
  }

}

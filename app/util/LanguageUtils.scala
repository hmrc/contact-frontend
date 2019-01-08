package util

import play.api.mvc.Flash

object LanguageUtils {

  val switchIndicatorKey       = "switching-language"
  val flashWithSwitchIndicator = Flash(Map(switchIndicatorKey -> "true"))

}

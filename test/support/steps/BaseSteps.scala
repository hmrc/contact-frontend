package support.steps

import org.scalatest.{MustMatchers, OptionValues}
import support.behaviour.NavigationSugar
import support.matchers.CustomMatchers


trait BaseSteps extends MustMatchers with OptionValues with CustomMatchers with NavigationSugar

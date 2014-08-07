package support.matchers

import org.scalatest.matchers.{MatchResult, Matcher}
import scala.annotation.tailrec

object CustomMatchers extends CustomMatchers

trait CustomMatchers {

  class ContainsAllTextsInOrderMatcher(toFind: Iterable[String]) extends Matcher[String] {

    def apply(left: String) = containsAllTextsInOrder(left, toFind)
  }

  def containInOrder(toFind: Iterable[String]) = new ContainsAllTextsInOrderMatcher(toFind)

  private def containsAllTextsInOrder(text: String, toFind: Iterable[String]): MatchResult = toFind match {
    case Nil => MatchResult(matches = true, "", "Found text")
    case x :: xs if x.contains("regex=") => {
      val regex = x.split("=")(1).r
      regex.findFirstMatchIn(text).fold(MatchResult(matches = false, s"Could not find regex '$regex' in:\n$text", "Found text")) { matcher =>
        containsAllTextsInOrder(text.substring(matcher.end), xs)
      }
    }
    case x :: xs if text.contains(x)  => containsAllTextsInOrder(text.substring(text.indexOf(x) + x.length), xs)
    case x :: _ => MatchResult(matches = false, s"Could not find '$x' in:\n$text", "Found text")
  }

}


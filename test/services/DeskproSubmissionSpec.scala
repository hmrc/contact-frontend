/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.DeskproSubmission.replaceRefererPath

class DeskproSubmissionSpec  extends AnyWordSpec with Matchers  {

  "replaceRefererPath" should {

    "replace the path if different from the referer value" in {
      val referer = "https://tax.gov.uk/service/accessibility-statement"
      val userAction = "/service/some-page"
      val res = replaceRefererPath(referer, Some(userAction))
      res should be ("https://tax.gov.uk" + userAction)
    }


    "use user action if referer isn't present" in {
      val userAction = "/service/some-page"
      val res = replaceRefererPath("", Some(userAction))
      res should be (userAction)
    }

    "use referer if userAction isn't present" in {
      val referer = "https://tax.gov.uk"
      val res = replaceRefererPath(referer,None)
      res should be (referer)
    }

    "append a forward slash if userAction is missing one" in {
      val referer = "https://tax.gov.uk/service/accessibility-statement"
      val userAction = "servicesome-page"
      val res = replaceRefererPath(referer, Some(userAction))
      res should be ("https://tax.gov.uk" + "/" + userAction)
    }

    "produces just path if referer is a blank string and userAction set" in {
      val userAction = "/servicesome-page/123"
      val res = replaceRefererPath("", Some(userAction))
      res should be (userAction)
    }

    "handles invalid uris in referer field" in {
      val referer = "hello world"
      val userAction = "/service/some-page"
      val res = replaceRefererPath(referer, Some(userAction))
      res should be (userAction)
    }

    "doesn't replace path if userAction is a empty string" in {
      val referer = "https://tax.gov.uk/service/accessibility-statement"
      val res = replaceRefererPath(referer, Some(""))
      res should be (referer)
    }

    "doesn't replace path if userAction is just whitespace" in {
      val referer = "https://tax.gov.uk/service/accessibility-statement"
      val res = replaceRefererPath(referer, Some("         "))
      res should be (referer)
    }
  }

}

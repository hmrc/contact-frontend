package unit.services

import org.scalatest.{Matchers, WordSpec}
import services.DeskproSubmission.rebuildReferer

class DeskproSubmissionSpec  extends WordSpec with Matchers  {

  "rebuildReferer" should {

    "replace the path if different from the referer value" in {
      val referer = "https://tax.gov.uk/service/accessibility-statement"
      val userAction = "/service/some-page"
      val res = rebuildReferer(Some(referer), Some(userAction))
      res should be ("https://tax.gov.uk" + userAction)
    }

    "not replace or append user action if referer already includes it" in {
      val referer = "https://tax.gov.uk/service/some-page"
      val userAction = "/service/some-page"
      val res = rebuildReferer(Some(referer), Some(userAction))
      res should be ("https://tax.gov.uk" + userAction)
    }

    "use user action if referer isn't present" in {
      val userAction = "/service/some-page"
      val res = rebuildReferer(None, Some(userAction))
      res should be (userAction)
    }

    "use referer if userAction isn't present" in {
      val referer = "https://tax.gov.uk"
      val res = rebuildReferer(Some(referer),None)
      res should be (referer)
    }

    "return N/A if neither are present" in {
      rebuildReferer(None, None) should be ("N/A")
    }

    "append a forward slash if userAction is missing one" in {
      val referer = "https://tax.gov.uk/service/accessibility-statement"
      val userAction = "servicesome-page"
      val res = rebuildReferer(Some(referer), Some(userAction))
      res should be ("https://tax.gov.uk" + "/" + userAction)
    }

    "produces just path if referer is a blank string and userAction set" in {
      val userAction = "/servicesome-page/123"
      val res = rebuildReferer(Some(""), Some(userAction))
      res should be (userAction)
    }

    "handles invalid uris in referer field" in {
      val referer = "hello world"
      val userAction = "/service/some-page"
      val res = rebuildReferer(Some(referer), Some(userAction))
      res should be (userAction)
    }

  }

  }

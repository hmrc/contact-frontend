/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.DeskproSubmission.replaceReferrerPath

class DeskproSubmissionSpec extends AnyWordSpec with Matchers {

  "replaceReferrerPath" should {

    "replace the path if different from the referrer value" in {
      val referrer   = "https://tax.gov.uk/service/accessibility-statement"
      val userAction = "/service/some-page"
      val res        = replaceReferrerPath(referrer, Some(userAction))
      res should be("https://tax.gov.uk" + userAction)
    }

    "use user action if referrer isn't present" in {
      val userAction = "/service/some-page"
      val res        = replaceReferrerPath("", Some(userAction))
      res should be(userAction)
    }

    "use referrer if userAction isn't present" in {
      val referrer = "https://tax.gov.uk"
      val res      = replaceReferrerPath(referrer, None)
      res should be(referrer)
    }

    "append a forward slash if userAction is missing one" in {
      val referrer   = "https://tax.gov.uk/service/accessibility-statement"
      val userAction = "servicesome-page"
      val res        = replaceReferrerPath(referrer, Some(userAction))
      res should be("https://tax.gov.uk" + "/" + userAction)
    }

    "produces just path if referrer is a blank string and userAction set" in {
      val userAction = "/servicesome-page/123"
      val res        = replaceReferrerPath("", Some(userAction))
      res should be(userAction)
    }

    "handles invalid uris in referrer field" in {
      val referrer   = "hello world"
      val userAction = "/service/some-page"
      val res        = replaceReferrerPath(referrer, Some(userAction))
      res should be(userAction)
    }

    "doesn't replace path if userAction is a empty string" in {
      val referrer = "https://tax.gov.uk/service/accessibility-statement"
      val res      = replaceReferrerPath(referrer, Some(""))
      res should be(referrer)
    }

    "doesn't replace path if userAction is just whitespace" in {
      val referrer = "https://tax.gov.uk/service/accessibility-statement"
      val res      = replaceReferrerPath(referrer, Some("         "))
      res should be(referrer)
    }
  }

}

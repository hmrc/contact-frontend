/*
 * Copyright 2022 HM Revenue & Customs
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

package views

import controllers.{ContactForm, ContactHmrcForm}
import model.FeedbackForm
import org.scalacheck.{Arbitrary, Gen}
import play.api.data.Form
import play.twirl.api.Html
import views.html.{ContactHmrcPage, FeedbackConfirmationPage, FeedbackPage}


class ContactFrontendAccessibilitySpec
    extends AutomaticAccessibilitySpec {

  // TODO we can currently generate Arbitrary[T] but not Arbitrary[T[_]], so need to provide these explicitly
  implicit lazy val arbContactForm: Arbitrary[Form[ContactForm]] = Arbitrary(Gen.const(ContactHmrcForm.form))
  implicit lazy val arbFeedbackForm: Arbitrary[Form[FeedbackForm]] = Arbitrary(Gen.const(controllers.FeedbackFormBind.form))

  val viewPackageName = "views.html"

  val renderViewByClass: PartialFunction[Any, Html] = {
    case contactHmrcPage: ContactHmrcPage => render(contactHmrcPage)
    case feedbackPage: FeedbackPage => render(feedbackPage)
    case feedbackConfirmationPage: FeedbackConfirmationPage => render(feedbackConfirmationPage)
  }

  runAccessibilityTests
}

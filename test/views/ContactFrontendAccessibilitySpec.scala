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
import model.{AccessibilityForm, FeedbackForm}
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.twirl.api.Html
import views.html._

// this is the class that any consuming team would have to implement
class ContactFrontendAccessibilitySpec
    extends AutomaticAccessibilitySpec {

  // TODO we can currently generate Arbitrary[T] but not Arbitrary[T[_]], so service teams need to provide these explicitly.
  // however, these should generally be available in the existing service codebase.
  // these are implicit to simplify calls to render() below
  implicit val arbContactForm: Arbitrary[Form[ContactForm]] = fixed(ContactHmrcForm.form)
  implicit val arbFeedbackForm: Arbitrary[Form[FeedbackForm]] = fixed(controllers.FeedbackFormBind.form)
  implicit val arbAccessibilityForm: Arbitrary[Form[AccessibilityForm]] = fixed(controllers.AccessibilityFormBind.form)

  // this is the package where the views live in the service
  val viewPackageName = "views.html"

  // this is the layout class which is injected into all full pages in the service
  val layoutClasses = Seq(classOf[views.html.components.Layout])

  // this partial function wires up the generic render() functions with arbitrary instances of the correct types.
  // IntelliJ invariably shows some or all of these as red :(
  override def renderViewByClass: PartialFunction[Any, Html] = {
    case contactHmrcPage: ContactHmrcPage => render(contactHmrcPage)
    case feedbackPage: FeedbackPage => render(feedbackPage)
    case feedbackConfirmationPage: FeedbackConfirmationPage => render(feedbackConfirmationPage)
    case contactHmrcConfirmationPage: ContactHmrcConfirmationPage => render(contactHmrcConfirmationPage)
    case accessibilityProblemPage: AccessibilityProblemPage => render(accessibilityProblemPage)
  }

  runAccessibilityTests()
}

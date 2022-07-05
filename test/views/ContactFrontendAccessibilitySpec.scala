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
import model.{AccessibilityForm, FeedbackForm, ReportProblemForm, SurveyForm}
import org.scalacheck.Arbitrary
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.twirl.api.Html
import play.api.data.Forms._
import views.html._

// this is the class that any consuming team would have to implement
class ContactFrontendAccessibilitySpec
    extends AutomaticAccessibilitySpec with MockitoSugar {

  val surveyForm: Form[SurveyForm] = Form[SurveyForm](mapping(
    "a" -> optional(number),
    "b" -> optional(number),
    "c" -> optional(text),
    "d" -> optional(text),
    "e" -> optional(text)
  )(SurveyForm.apply)(SurveyForm.unapply))

  val reportProblemForm: Form[ReportProblemForm] = Form[ReportProblemForm](mapping(
    "a" -> text,
    "b" -> text,
    "c" -> text,
    "d" -> text,
    "e" -> boolean,
    "f" -> optional(text),
    "g" -> optional(text),
    "h" -> text,
    "i" -> optional(text)
  )(ReportProblemForm.apply)(ReportProblemForm.unapply))

  // TODO we can currently generate Arbitrary[T] but not Arbitrary[T[_]], so service teams need to provide these explicitly.
  // however, these should generally be available in the existing service codebase.
  // these are implicit to simplify calls to render() below
  implicit val arbReportProblemPage: Arbitrary[Form[ReportProblemForm]] = fixed(reportProblemForm)
  implicit val arbSurveyForm: Arbitrary[Form[SurveyForm]] = fixed(surveyForm)
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
    case internalErrorPage: InternalErrorPage => render(internalErrorPage)
    case accessibilityProblemPage: AccessibilityProblemPage => render(accessibilityProblemPage)
    case surveyPage: SurveyPage => render(surveyPage)
//    case errorPage: ErrorPage => render(errorPage)
    case reportProblemConfirmationPage: ReportProblemConfirmationPage => render(reportProblemConfirmationPage)
    case surveyConfirmationPage: SurveyConfirmationPage => render(surveyConfirmationPage)
    case reportProblemPage: ReportProblemPage => render(reportProblemPage)
  }

  runAccessibilityTests()
}

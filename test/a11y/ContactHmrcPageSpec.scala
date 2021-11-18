/*
 * Copyright 2021 HM Revenue & Customs
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

package a11y

import _root_.helpers.{ApplicationSupport, MessagesSupport}
import config.AppConfig
import controllers.ContactForm
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.ContactHmrcPage

class ContactHmrcPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with AccessibilityMatchers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/contact-hmrc").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val contactHmrcForm: Form[ContactForm] = Form[ContactForm](
    mapping(
      "contact-name"     -> text.verifying("contact.name.error.required", msg => msg.nonEmpty),
      "contact-email"    -> text.verifying("contact.email.error.required", msg => msg.nonEmpty),
      "contact-comments" -> text.verifying("contact.comments.error.required", msg => msg.nonEmpty),
      "isJavascript"     -> boolean,
      "referrer"         -> text,
      "csrfToken"        -> text,
      "service"          -> optional(text),
      "userAction"       -> optional(text)
    )(ContactForm.apply)(ContactForm.unapply)
  )

  val action: Call = Call(method = "POST", url = "/contact/contact-hmrc/submit")

  "the Contact Hmrc standalone page" should {
    val contactHmrcPage = app.injector.instanceOf[ContactHmrcPage]
    val content         = contactHmrcPage(contactHmrcForm, action)

    "pass accessibility checks" in {
      content.toString() should passAccessibilityChecks
    }
  }
}

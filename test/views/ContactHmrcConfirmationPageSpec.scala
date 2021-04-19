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

package views

import _root_.helpers.{ApplicationSupport, JsoupHelpers, MessagesSupport}
import config.AppConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import views.html.{ContactHmrcConfirmationPage, ProblemReportsNonjsConfirmationPage}

class ContactHmrcConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers {

  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/submit")
  implicit lazy val messages: Messages         = getMessages(app, fakeRequest)
  implicit lazy val appConfig: AppConfig       = app.injector.instanceOf[AppConfig]

  "the Contact Hmrc standalone confirmation page" should {
    val confirmationPage = app.injector.instanceOf[ContactHmrcConfirmationPage]
    val content          = confirmationPage()

    "include the H1 element with page title" in {
      val heading1 = content.select("h1")
      heading1            should have size 1
      heading1.first.text should be("Help and contact")
    }

    "include the H2 element with thanks" in {
      val heading2 = content.select("h2")
      heading2.first.text should be("Thank you")
    }

    "include the paragraph body element with confirmation submission" in {
      val paragraph = content.select("p.govuk-body")
      paragraph.first.text should be("Someone will get back to you within 2 working days.")
    }

    "translate the title into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = confirmationPage()

      val titles = welshContent.select("h1")
      titles.first.text should be("Cymorth a chysylltiadau")
    }
  }
}

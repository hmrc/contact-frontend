/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package views

import _root_.helpers.{JsoupHelpers, MessagesSupport}
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
    with GuiceOneAppPerSuite
    with MessagesSupport
    with JsoupHelpers {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

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

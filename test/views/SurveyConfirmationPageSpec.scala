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
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import views.html.SurveyConfirmationPage

class SurveyConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MessagesSupport
    with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo")
  implicit lazy val messages: Messages         = getMessages(app, fakeRequest)
  implicit lazy val appConfig: AppConfig       = app.injector.instanceOf[AppConfig]

  "the feedback confirmation page" should {
    val feedbackConfirmationPage = app.injector.instanceOf[SurveyConfirmationPage]
    val content                  = feedbackConfirmationPage()

    "include the confirmation panel" in {
      val panels = content.select("h1")
      panels            should have size 1
      panels.first.text should be("Thank you, your feedback has been received.")
    }

    "translate the title into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = feedbackConfirmationPage()

      val titles = welshContent.select("h1")
      titles.first.text should be("Diolch, mae eich adborth wedi dod i law.")
    }
  }
}

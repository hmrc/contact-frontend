/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package views

import config.AppConfig
import _root_.helpers.{AppHelpers, JsoupHelpers}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import views.html.AccessibilityProblemConfirmationPage

class AccessibilityProblemConfirmationPageSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with AppHelpers
    with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo")
  implicit lazy val messages: Messages         = getMessages(app, fakeRequest)
  implicit lazy val appConfig: AppConfig       = app.injector.instanceOf[AppConfig]

  "the report an accessibility problem confirmation page" should {
    val accessibilityProblemConfirmationPage = app.injector.instanceOf[AccessibilityProblemConfirmationPage]
    val content                              = accessibilityProblemConfirmationPage()

    "include the confirmation panel" in {
      val panels = content.select("h1.govuk-panel__title")
      panels            should have size 1
      panels.first.text should be("Your accessibility problem has been reported.")
    }

    "include the what happens next section" in {
      val headings = content.select("h2.govuk-heading-l")
      headings            should have size 1
      headings.first.text should be("What happens next")
    }

    "translate the title into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = accessibilityProblemConfirmationPage()

      val titles = welshContent.select("h1.govuk-panel__title")
      titles.first.text should be("Mae'ch problem wedi ei nodi")
    }
  }
}

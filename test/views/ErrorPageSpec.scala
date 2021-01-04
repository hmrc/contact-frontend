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
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ErrorPage

class ErrorPageSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with MessagesSupport with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "the error template" should {
    val errorTemplate = app.injector.instanceOf[ErrorPage]
    val content       =
      errorTemplate(pageTitle = "This is the title", heading = "This is the heading", message = "This is the message.")

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "not include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 0
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "This is the title"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "This is the heading"
    }

    "return the introductory content" in {
      contentAsString(content) should include(
        "This is the message."
      )
    }
  }
}

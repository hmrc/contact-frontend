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
import views.html.DeskproErrorPage

class DeskproErrorPageSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MessagesSupport
    with JsoupHelpers {
  implicit lazy val fakeRequest: RequestHeader = FakeRequest("GET", "/foo").withCSRFToken

  implicit lazy val messages: Messages = getMessages(app, fakeRequest)

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "the error template" should {
    val errorTemplate = app.injector.instanceOf[DeskproErrorPage]
    val content       =
      errorTemplate()

    "include the hmrc banner" in {
      val banners = content.select(".hmrc-organisation-logo")

      banners            should have size 1
      banners.first.text should be("HM Revenue & Customs")
    }

    "include the hmrc language toggle" in {
      val languageSelect = content.select(".hmrc-language-select")
      languageSelect should have size 1
    }

    "display the correct browser title" in {
      content.select("title").text shouldBe "Sorry, we’re experiencing technical difficulties"
    }

    "display the correct page heading" in {
      val headers = content.select("h1")
      headers.size       shouldBe 1
      headers.first.text shouldBe "Sorry, we’re experiencing technical difficulties"
    }

    "return the deskpro specific content" in {
      contentAsString(content) should include(
        "There was a problem sending your query."
      )
    }

    "return the generic content" in {
      contentAsString(content) should include(
        "Please try again later."
      )
    }
  }
}

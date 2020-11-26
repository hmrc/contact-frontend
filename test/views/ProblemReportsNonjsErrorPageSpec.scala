/*
 * Copyright 2020 HM Revenue & Customs
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
import views.html.ProblemReportsNonjsErrorPage

class ProblemReportsNonjsErrorPageSpec
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

  "the Problem Reports standalone error page" should {
    val errorPage = app.injector.instanceOf[ProblemReportsNonjsErrorPage]
    val content   = errorPage()

    "include the H1 element with page title" in {
      val heading1 = content.select("h1")
      heading1            should have size 1
      heading1.first.text should be("There was a problem sending your query")
    }

    "include the paragraph body element with confirmation submission" in {
      val paragraph = content.select("p.govuk-body")
      paragraph.first.text should be("Please try again later.")
    }

    "translate the title into Welsh if requested" in {
      implicit val messages: Messages = getWelshMessages
      val welshContent                = errorPage()

      val titles = welshContent.select("h1")
      titles.first.text should be("Cafwyd problem wrth anfon eich ymholiad")
    }
  }
}

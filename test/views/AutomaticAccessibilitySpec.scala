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

import _root_.helpers.{ApplicationSupport, ArbDerivation, JsoupHelpers, MessagesSupport}
import config.AppConfig
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.{Call, RequestHeader}
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers


trait AutomaticAccessibilitySpec
  extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MessagesSupport
    with JsoupHelpers
    with AccessibilityMatchers
    with ViewDiscovery
    with ArbDerivation
    with TemplateRenderers {

  // this has to be overridden by consuming teams
  def renderViewByClass: PartialFunction[Any, Html] = PartialFunction.empty

  // these are things that need to have sane values for pages to render properly - there may be others
  val fakeRequest: RequestHeader = FakeRequest("GET", "/contact-hmrc").withCSRFToken
  val messages: Messages = getMessages(app, fakeRequest)
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val call: Call = Call(method = "POST", url = "/some/url")

  implicit lazy val arbRequest: Arbitrary[RequestHeader] = Arbitrary(Gen.const(fakeRequest))
  implicit lazy val arbMessages: Arbitrary[Messages] = Arbitrary(Gen.const(messages))
  implicit lazy val arbConfig: Arbitrary[AppConfig] = Arbitrary(Gen.const(appConfig))
  implicit lazy val arbCall: Arbitrary[Call] = Arbitrary(Gen.const(call))

  lazy val runAccessibilityTests: Unit = {
    viewNames foreach { viewName =>
      // get the class by name from the classloader, then get an instance of the class from the Play app
      val clazz = app.classloader.loadClass(viewName.toString)
      val viewInstance = app.injector.instanceOf(clazz)

      // dynamically generate scalatest for each view
      viewName.toString should {
        "be accessible" in {
          // if a view hasn't been 'wired up' yet, this will mark the test as pending, with wiring instructions
          val markAsPendingWithImplementationGuidance: PartialFunction[Any, Any] = {
            case _ =>
              println("Missing wiring - add the following to your renderViewByClass function:\n" +
                s"    case ${viewName.instanceName}: ${viewName.className} => render(${viewName.instanceName})")
              pending
          }

          val renderOrMarkAsPending = renderViewByClass orElse markAsPendingWithImplementationGuidance

          // render the view, and strip any leading whitespace
          val html = renderOrMarkAsPending(viewInstance)
          val pageContent = html.asInstanceOf[Html].toString.trim

          // test the page for accessibility
          pageContent should passAccessibilityChecks
        }
      }
    }
  }

}

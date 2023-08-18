/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import org.scalatest.TestData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status.GONE
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}

import scala.concurrent.Future
import scala.language.implicitConversions

class DisableLoadingPartialsSpec extends AnyWordSpec with GuiceOneAppPerTest with Matchers {

  val serviceName = "my-service"

  implicit override def newAppForTest(testData: TestData): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"      -> false,
        "metrics.enabled"  -> false,
        "auditing.enabled" -> false,
        "disablePartials"  -> true
      )
      .build()

  "ReportProblemControllerSpec" should {

    "returns 410 when partialIndex is called " in {
      val result: Future[Result] = app.injector
        .instanceOf[ReportProblemController]
        .partialIndex(
          preferredCsrfToken = None,
          service = Some(serviceName)
        )(FakeRequest())

      status(result) should be(GONE)
    }

    "returns 410 when partialAjaxIndex is called" in {
      val result: Future[Result] = app.injector
        .instanceOf[ReportProblemController]
        .partialAjaxIndex(
          service = Some(serviceName)
        )(FakeRequest())

      status(result) should be(GONE)
    }
  }

  "ContactHmrcController" should {
    "returns 410 when partialIndex is called" in {
      val result: Future[Result] = app.injector
        .instanceOf[ContactHmrcController]
        .partialIndex(
          submitUrl = "",
          csrfToken = "",
          service = Some(serviceName),
          renderFormOnly = true
        )(FakeRequest())

      status(result) should be(GONE)
    }
  }

  "FeedbackController" should {
    "returns 410 when partialIndex is called" in {
      val result: Future[Result] = app.injector
        .instanceOf[FeedbackController]
        .partialIndex(
          submitUrl = "",
          csrfToken = "",
          service = Some(serviceName),
          referer = None,
          canOmitComments = true,
          referrerUrl = None
        )(FakeRequest())

      status(result) should be(GONE)
    }
  }
}

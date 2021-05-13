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

package test

import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{DefaultWSCookie, WSClient, WSResponse}
import play.api.test.Helpers._

class CSRFIntegrationSpec extends AnyWordSpec with Matchers with WireMockEndpoints with GuiceOneServerPerSuite {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"                             -> false,
        "metrics.enabled"                         -> false,
        "auditing.enabled"                        -> false,
        "microservice.services.hmrc-deskpro.port" -> endpointPort
      )
      .build()

  private def problemReportsSecureUrl = s"http://localhost:$port/contact/problem_reports_secure"
  private def problemReportsNonjsUrl  = s"http://localhost:$port/contact/problem_reports_nonjs"
  private def feedbackUrl             = s"http://localhost:$port/contact/beta-feedback-unauthenticated"
  private def contactHmrcUrl          = s"http://localhost:$port/contact/contact-hmrc-unauthenticated"
  private def surveyUrl               = s"http://localhost:$port/contact/survey?ticketId=GFBN-8051-KLNY&serviceId=foo"
  private def accessibilityUrl        = s"http://localhost:$port/contact/accessibility-unauthenticated"

  private val problemForm = Map(
    "report-name"   -> "Joe",
    "report-email"  -> "things@example.com",
    "report-action" -> "Doing stuff",
    "report-error"  -> "Things broken"
  )

  private val contactForm = Map(
    "referrer"         -> "https://www.example.com",
    "contact-name"     -> "Fred",
    "contact-email"    -> "fred@example.com",
    "contact-comments" -> "Hello"
  )

  private val feedbackForm = Map(
    "referrer"          -> "https://www.example.com",
    "feedback-name"     -> "Mary",
    "feedback-email"    -> "mary@example.com",
    "feedback-rating"   -> "5",
    "feedback-comments" -> "Some feedback"
  )

  private val accessibilityForm = Map(
    "referrer"           -> "https://www.example.com",
    "name"               -> "Jack",
    "email"              -> "jack@example.com",
    "problemDescription" -> "I cannot use this"
  )

  private val surveyForm = Map(
    "helpful" -> "1",
    "speed"   -> "5"
  )

  private val anyCookie = DefaultWSCookie("cookieName", "cookieValue")

  private def getCsrfToken(response: WSResponse) =
    Jsoup.parse(response.body).select("input[name=csrfToken]").first.attr("value")

  private val wsClient = app.injector.instanceOf[WSClient]

  private def get(url: String) = await(
    wsClient
      .url(url)
      .get()
  )

  private def postWithAnyCookie(url: String, form: Map[String, String]) =
    await(
      wsClient
        .url(url)
        .addCookies(anyCookie)
        .post(form)
    )

  private def postWithSessionAndCsrfToken(url: String, form: Map[String, String], previousResponse: WSResponse) = {
    val mdtp: String = previousResponse.cookie("mdtp").get.value
    val csrfToken    = getCsrfToken(previousResponse)

    await(
      wsClient
        .url(url)
        .addCookies(DefaultWSCookie("mdtp", mdtp))
        .post(
          form ++
            Map(
              "csrfToken" -> csrfToken
            )
        )
    )
  }

  "The Play application's CSRF configuration" should {
    "respond with a 403 for problem_reports_secure if no CSRF token present" in {
      val response = postWithAnyCookie(problemReportsSecureUrl, problemForm)

      response.status should be(FORBIDDEN)
    }

    "respond with a 200 for problem_reports_secure if X-Requested-With header is supplied with any value" in {
      val response = await(
        wsClient
          .url(problemReportsSecureUrl)
          .withHttpHeaders("X-Requested-With" -> "something")
          .addCookies(anyCookie)
          .post(
            problemForm
          )
      )

      response.status should be(OK)
    }

    "respond with a 403 for problem_reports_secure if a Csrf-Token header is supplied with an arbitrary value" in {
      val response = await(
        wsClient
          .url(problemReportsSecureUrl)
          .withHttpHeaders("Csrf-Token" -> "sausage")
          .addCookies(anyCookie)
          .post(
            problemForm
          )
      )

      response.status should be(FORBIDDEN)
    }

    "respond with a 200 for problem_reports_secure if a Csrf-Token header is supplied with the value 'nocheck'" in {
      val response = await(
        wsClient
          .url(problemReportsSecureUrl)
          .withHttpHeaders("Csrf-Token" -> "nocheck")
          .addCookies(anyCookie)
          .post(
            problemForm
          )
      )

      response.status should be(OK)
    }

    "respond with a 403 for problem_reports_nonjs if any cookie but no CSRF token is present" in {
      val response = postWithAnyCookie(problemReportsNonjsUrl, problemForm)

      response.status should be(FORBIDDEN)
    }

    "respond with 200 for problem_reports_nonjs if a CSRF token is present" in {
      val getResponse = get(problemReportsNonjsUrl)
      getResponse.status should be(OK)

      val postResponse = postWithSessionAndCsrfToken(problemReportsNonjsUrl, problemForm, getResponse)
      postResponse.status should be(OK)
    }

    "respond with 403 for problem_reports_nonjs if a CSRF token from a different session is provided" in {
      val getResponse = get(problemReportsNonjsUrl)
      getResponse.status should be(OK)
      val csrfToken = getCsrfToken(getResponse)

      val getResponse2 = await(
        wsClient
          .url(problemReportsNonjsUrl)
          .get()
      )
      getResponse2.status should be(OK)
      val mdtp2: String = getResponse2.cookie("mdtp").get.value

      val postResponse =
        await(
          wsClient
            .url(problemReportsNonjsUrl)
            .addCookies(DefaultWSCookie("mdtp", mdtp2))
            .post(
              problemForm ++
                Map(
                  "csrfToken" -> csrfToken
                )
            )
        )

      postResponse.status should be(FORBIDDEN)
    }

    "respond with 403 for contact-hmrc-unauthenticated when no CSRF token is present" in {
      val response = postWithAnyCookie(contactHmrcUrl, contactForm)

      response.status should be(FORBIDDEN)
    }

    "respond with 200 for contact-hmrc-unauthenticated if a CSRF token is present" in {
      val getResponse = get(contactHmrcUrl)
      getResponse.status should be(OK)

      val postResponse = postWithSessionAndCsrfToken(contactHmrcUrl, contactForm, getResponse)

      postResponse.status should be(OK)
    }

    "respond with 403 for feedback-unauthenticated if no CSRF token is present" in {
      val response = postWithAnyCookie(feedbackUrl, feedbackForm)

      response.status should be(FORBIDDEN)
    }

    "respond with 200 for feedback-unauthenticated if a CSRF token is provided" in {
      val getResponse = get(feedbackUrl)
      getResponse.status should be(OK)

      val postResponse = postWithSessionAndCsrfToken(feedbackUrl, feedbackForm, getResponse)

      postResponse.status should be(OK)
    }

    "respond with 403 for accessibility-unauthenticated if no CSRF token is present" in {
      val response = postWithAnyCookie(accessibilityUrl, accessibilityForm)

      response.status should be(FORBIDDEN)
    }

    "respond with 200 for accessibility-unauthenticated if a CSRF token is provided" in {
      val getResponse = get(accessibilityUrl)
      getResponse.status should be(OK)

      val postResponse = postWithSessionAndCsrfToken(accessibilityUrl, accessibilityForm, getResponse)

      postResponse.status should be(OK)
    }

    "respond with 403 for survey if no CSRF token is present" in {
      val response = postWithAnyCookie(surveyUrl, surveyForm)

      response.status should be(FORBIDDEN)
    }
  }
}

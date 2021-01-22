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

package controllers

import config.AppConfig
import play.api.Configuration
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.auth.core.NoActiveSession

import scala.concurrent.{ExecutionContext, Future}

trait LoginRedirection extends Results {

  def appConfig: AppConfig

  def configuration: Configuration

  def loginRedirection[B <: Result](
    continueUrl: String
  )(body: Future[B])(implicit ec: ExecutionContext): Future[Result] =
    body.recoverWith { case _: NoActiveSession =>
      Future.successful(redirectForLogin(continueUrl))
    }

  private def redirectForLogin(continueUrl: String): Result = {

    val origin: String = configuration
      .getOptional[String]("sosOrigin")
      .orElse(configuration.getOptional[String]("appName"))
      .getOrElse("undefined")

    lazy val companyAuthUrl = configuration.getOptional[String](s"company-auth.host").getOrElse("")
    val loginURL: String    = s"$companyAuthUrl/gg/sign-in"
    val continueURL: String = appConfig.loginCallback(continueUrl)

    Redirect(loginURL, Map("continue" -> Seq(continueURL), "origin" -> Seq(origin)))

  }

}

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

package config

import javax.inject.Inject
import play.api.Configuration

trait AppConfig {

  def contactHmrcAboutTaxUrl: String
  def externalReportProblemUrl: String
  def backUrlDestinationAllowList: Set[String]
  def loginCallback(continueUrl: String): String
  def enableLanguageSwitching: Boolean
  def useRefererFromRequest: Boolean
  def disablePartials: Boolean
  def disableAjaxPartials: Boolean
  def useDeskproTicketQueue: Boolean
  def sendExplicitAuditEvents: Boolean

}

class CFConfig @Inject() (configuration: Configuration) extends AppConfig {

  private def loadConfigString(key: String): String =
    configuration
      .getOptional[String](key)
      .getOrElse(configNotFoundError(key))

  private val contactHost = configuration
    .getOptional[String]("contact-frontend.host")
    .getOrElse("")

  override def contactHmrcAboutTaxUrl: String =
    loadConfigString("contactHmrcAboutTax.url")

  override lazy val externalReportProblemUrl =
    s"$contactHost/contact/problem_reports"

  override lazy val backUrlDestinationAllowList =
    loadConfigString("backUrlDestinationAllowList")
      .split(',')
      .filter(_.nonEmpty)
      .toSet

  override def loginCallback(continueUrl: String) = s"$contactHost$continueUrl"

  override def enableLanguageSwitching =
    configuration
      .getOptional[Boolean]("enableLanguageSwitching")
      .getOrElse(false)

  override def disablePartials: Boolean =
    configuration
      .getOptional[Boolean]("disablePartials")
      .getOrElse(false)

  override def disableAjaxPartials: Boolean =
    configuration
      .getOptional[Boolean]("disableAjaxPartials")
      .getOrElse(false)

  override def useDeskproTicketQueue: Boolean =
    configuration.getOptional[Boolean]("useDeskproTicketQueue").getOrElse(false)

  override def useRefererFromRequest: Boolean = configuration.getOptional[Boolean]("useRefererHeader").getOrElse(false)

  private def configNotFoundError(key: String) =
    throw new RuntimeException(s"Could not find config key '$key'")

  override def sendExplicitAuditEvents: Boolean =
    configuration.getOptional[Boolean]("sendExplicitAuditEvents").getOrElse(false)

}

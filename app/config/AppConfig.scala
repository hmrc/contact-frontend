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

  def backUrlDestinationAllowList: Set[String]
  def sendExplicitAuditEvents: Boolean
  def enableOlfgComplaintsEndpoints: Boolean
  def urlWithPlatformHost(url: String): String

}

class CFConfig @Inject() (configuration: Configuration) extends AppConfig {

  private def loadConfigString(key: String): String =
    configuration
      .getOptional[String](key)
      .getOrElse(configNotFoundError(key))

  private val platformHost: String = configuration
    .getOptional[String]("platform.frontend.host")
    .getOrElse("")

  override def urlWithPlatformHost(url: String): String = s"$platformHost$url"

  override lazy val backUrlDestinationAllowList: Set[String] =
    loadConfigString("backUrlDestinationAllowList")
      .split(',')
      .filter(_.nonEmpty)
      .toSet

  private def configNotFoundError(key: String): Nothing =
    throw new RuntimeException(s"Could not find config key '$key'")

  override def sendExplicitAuditEvents: Boolean =
    configuration.getOptional[Boolean]("sendExplicitAuditEvents").getOrElse(false)

  override def enableOlfgComplaintsEndpoints: Boolean =
    configuration.getOptional[Boolean]("enableOlfgComplaintsEndpoints").getOrElse(false)

}

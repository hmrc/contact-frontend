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

package util

import scala.util.matching.Regex

case class DeskproEmailValidator() {

  // The regexes used in this validator are based on PCRE regexes provided by Deskpro via DDC Ops in December 2020

  def validate(email: String): Boolean =
    !email.endsWith("@") && {
      email.split("@").toList match {
        case name :: domain :: Nil => validateName(name) && (validateDomain(domain) || validateIp(domain))
        case _                     => false
      }
    }

  private def validateName(name: String): Boolean = {
    val validNamePattern: Regex = """(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:[\\.][a-z0-9!#$%&'*+/=?^_`{|}~-]+)*$""".r
    validNamePattern.findFirstIn(name).isDefined
  }

  def validateDomain(domain: String): Boolean = {
    val validDomainPattern =
      """(?i)^(?:(?:(?:(?:[a-zA-Z0-9][-a-zA-Z0-9]*)?[a-zA-Z0-9])[\\.])*(?:[a-zA-Z][-a-zA-Z0-9]*[a-zA-Z0-9]|[a-zA-Z]))$""".r
    validDomainPattern.findFirstIn(domain).isDefined
  }

  def validateIp(domain: String): Boolean = {
    val validIpPattern =
      """^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[\\.]){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""".r
    validIpPattern.findFirstIn(domain).isDefined
  }
}

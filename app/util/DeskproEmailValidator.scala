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

import config.ValidationConstants
import scala.util.matching.Regex

object DeskproEmailValidator {
  import ValidationConstants._

  // The regexes used in this validator are based on PCRE regexes provided by Deskpro via DDC Ops in December 2020
  // They have been updated in July 2026 as Deskpro have confirmed via Slack that domains without a "." are not considered valid
  // Updated in October 2026 following manual testing to ensure that emails with newline characters are not accepted

  private val validNamePattern: Regex   =
    """(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:[\\.][a-z0-9!#$%&'*+/=?^_`{|}~-]+)*$""".r
  private val validDomainPattern: Regex =
    """(?i)^(?:(?:(?:(?:[a-zA-Z0-9][-a-zA-Z0-9]*)?[a-zA-Z0-9])[\\.])+(?:[a-zA-Z][-a-zA-Z0-9]*[a-zA-Z0-9]|[a-zA-Z]))$""".r
  private val validIpPattern: Regex     =
    """^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[\\.]){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""".r

  def validate(email: String): Boolean =
    !email.endsWith("@") &&
      !email.contains('\n') &&
      !email.contains("\\") &&
      email.length <= maxEmailLength && {
        email.split("@").toList match {
          case name :: domain :: Nil =>
            validateName(name) && (validateDomain(domain) || validIpPattern.matches(domain))
          case _                     => false
        }
      }

  private def validateName(name: String): Boolean =
    name.length <= maxEmailNameLength && validNamePattern.matches(name)

  private def validateDomain(domain: String): Boolean =
    validDomainPattern.matches(domain) && domain.split('.').forall(_.length <= maxEmailDomainLabelLength)
}

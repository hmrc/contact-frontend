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

import play.api.data.Forms.text
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Forms, Mapping}
import play.api.i18n.{Lang, MessagesApi}
import util.DateValidationSupport.{dateConstraint, dayConstraint, monthConstraint, yearConstraint}

import scala.util.{Failure, Success, Try}

object DateValidationSupport {

  val dayConstraint: Constraint[String] = Constraint("constraints.day") { dayAsString =>
    val validationErrors: Seq[ValidationError] = Try(dayAsString.toInt) match {
      case Success(dayAsInt) =>
        if (dayAsInt < 1 || dayAsInt > 31) Seq(ValidationError("Value outside range of days"))
        else Nil
      case Failure(_)        => Seq(ValidationError("Value entered must be a number"))
    }

    if (validationErrors.isEmpty) {
      Valid
    } else {
      Invalid(validationErrors)
    }
  }

  def monthConstraint()(implicit messagesApi: MessagesApi, lang: Lang): Constraint[String] = Constraint("constraints.month") { monthAsString =>
    val validationErrors: Seq[ValidationError] =
      if (isInListOfAcceptedMonths(monthAsString)) Nil else {
        Try(monthAsString.toInt) match {
          case Success(monthAsInt) =>
            if (monthAsInt < 1 || monthAsInt > 12) Seq(ValidationError("Value outside range of month"))
            else Nil
          case Failure(_) => Seq(ValidationError("Value entered for month is invalid"))
        }
      }

    if (validationErrors.isEmpty) {
      Valid
    } else {
      Invalid(validationErrors)
    }
  }

  private def isInListOfAcceptedMonths(userInput: String)(implicit messagesApi: MessagesApi, lang: Lang) = {
    val messages: Map[String, String] = messagesApi.messages(lang.code)
    val februaryLong: String = messages("february").toLowerCase
    val februaryAbbreviated: String = messages("february.abbrv").toLowerCase
    val acceptableMonths = Seq(februaryAbbreviated, februaryLong)
    acceptableMonths.contains(userInput.toLowerCase)
  }

  val yearConstraint: Constraint[String] = Constraint("constraints.year") { yearAsString =>
    val validationErrors: Seq[ValidationError] = Try(yearAsString.toInt) match {
      case Success(yearAsInt) =>
        if (yearAsInt < 1900) Seq(ValidationError("Value outside range of year"))
        else Nil
      case Failure(_)         => Seq(ValidationError("Value entered must be a number"))
    }

    if (validationErrors.isEmpty) {
      Valid
    } else {
      Invalid(validationErrors)
    }
  }

  val dateConstraint: Constraint[(String, String, String)] = Constraint("constraints.date") { dateData =>
    val validationErrors: Seq[ValidationError] =
      if (dateData == ("14", "Feb", "2000") || dateData == ("14", "February", "2000") ||
          dateData == ("14", "Chwef", "2000") || dateData == ("14", "Chwefror", "2000")) Nil
      else {
        println(s"dateAsString is: $dateData")
        Seq(ValidationError("Nope, wrong"))
      }

    if (validationErrors.isEmpty) {
      Valid
    } else {
      Invalid(validationErrors)
    }
  }
}

case class DateData(day: String, month: String, year: String)(implicit messagesApi: MessagesApi)

object DateData {
  def dayMapping(str: String)   = text.verifying(dayConstraint)
  def monthMapping(str: String)(implicit messagesApi: MessagesApi, lang: Lang) = text.verifying(monthConstraint)
  def yearMapping(str: String)  = text.verifying(yearConstraint)

  def mapping(prefix: String)(implicit messagesApi: MessagesApi, lang: Lang): Mapping[DateData] =
    Forms
      .tuple(
        "day"   -> dayMapping(prefix),
        "month" -> monthMapping(prefix),
        "year"  -> yearMapping(prefix)
      )
      .verifying(dateConstraint)
      .transform(
        validatedDateTuple => DateData(validatedDateTuple._1, validatedDateTuple._2, validatedDateTuple._3),
        dateData => (dateData.day, dateData.month, dateData.year)
      )

}

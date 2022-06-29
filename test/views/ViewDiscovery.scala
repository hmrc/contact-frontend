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

trait ViewDiscovery {

  def viewPackageName: String

  case class ViewName(value: String) {
    override def toString: String = value

    def className: String = value.split("\\.").toList.last

    def instanceName: String = camelcase(className)

    private def camelcase(s: String): String = s.toList match {
      case c :: tail => (c.toString.toLowerCase + tail.mkString).mkString
    }
  }

  lazy val viewNames: Seq[ViewName] = Seq(
    "ContactHmrcPage",
    "ErrorPage",
    "FeedbackConfirmationPage",
    "FeedbackPage",
    "SurveyPage"
  ).map(name => ViewName(viewPackageName + "." + name)) // TODO these would not be hardcoded, but would come from classloader/filesystem

}

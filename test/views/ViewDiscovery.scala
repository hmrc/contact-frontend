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

import org.reflections.Reflections
import org.reflections.scanners.Scanners._

import scala.jdk.CollectionConverters._


trait ViewDiscovery {

  // this has to be implemented by consuming teams - could there be views in more than one package?
  def viewPackageName: String

  // value class to simplify test code
  case class ViewName(value: String) {
    override def toString: String = value

    def className: String = value.split("\\.").toList.last

    def instanceName: String = camelcase(className)

    private def camelcase(s: String): String = s.toList match {
      case c :: tail => (c.toString.toLowerCase + tail.mkString).mkString
    }
  }

  // may be better ways to do this... but this has a simple API for our PoC
  // lazy vals due to initialisation order (viewPackageName is defined in the team's test class that extends this)
  lazy val reflections = new Reflections(viewPackageName)

  lazy val viewNames: Seq[ViewName] = reflections
    .get(SubTypes.of("play.twirl.api.BaseScalaTemplate").asClass())
    .asScala
    .toSeq
    .map(_.getName)
    .filter(_.endsWith("Page")) // TODO maybe regex filter(s) that teams can override?
    .map(ViewName)

}

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

  // layout class(es) - basically marker class(es) for every complete html page in a service
  def layoutClasses: Seq[Class[_]]

  // value class to simplify test code
  case class ViewName(value: String) {
    override def toString: String = value

    def className: String = value.split("\\.").toList.last

    def instanceName: String = camelcase(className)

    private def camelcase(s: String): String = s.toList match {
      case c :: tail => (c.toString.toLowerCase + tail.mkString).mkString
    }
  }

  def viewNames(baseType: String = "play.twirl.api.BaseScalaTemplate"): Seq[ViewName] = {
  // may be better ways to do this... but this has a simple API for our PoC
    new Reflections(viewPackageName)
      .get(SubTypes.of(baseType).asClass())
      .asScala
      .toSeq
      .filter(_.getConstructors.toSeq.exists(_.getParameterTypes.toSeq.exists(layoutClasses.contains(_))))
      .map(_.getName)
      .sorted
      .map(ViewName)
  }

}

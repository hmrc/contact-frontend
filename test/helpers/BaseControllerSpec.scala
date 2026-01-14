/*
 * Copyright 2026 HM Revenue & Customs
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

package helpers

import config.{AppConfig, CFConfig}
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

trait BaseControllerSpec
    extends AnyWordSpec
    with Matchers
    with ApplicationSupport
    with MockitoSugar
    with BeforeAndAfterEach {

  def instanceOf[A: ClassTag]: A = app.injector.instanceOf[A]

  given Messages         = instanceOf[MessagesApi].preferred(Seq(Lang("en")))
  given AppConfig        = new CFConfig(app.configuration)
  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = any[HeaderCarrier]
}

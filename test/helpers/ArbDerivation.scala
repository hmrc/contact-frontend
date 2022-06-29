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

package helpers

import magnolia1._
import org.scalacheck.Gen.Parameters
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

import scala.language.experimental.macros

// this is lifted from https://tech.ovoenergy.com/scalacheck-magnolia/
// it generates Arbitrary case class instances
trait ArbDerivation {
  def parameters: Parameters

  implicit def gen[T]: Arbitrary[T] = macro Magnolia.gen[T]

  type Typeclass[T] = Arbitrary[T]

  def join[T](ctx: CaseClass[Arbitrary, T]): Arbitrary[T] = {
    val t: T = ctx.construct { param: Param[Typeclass, T] =>
      param
        .typeclass
        .arbitrary
        .pureApply(parameters, Seed.random())
    }
    Arbitrary(Gen.delay(t))
  }
}
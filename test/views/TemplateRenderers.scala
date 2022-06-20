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

import org.scalacheck.Arbitrary
import org.scalacheck.rng.Seed
import play.twirl.api.{Html, Template4, Template5}

trait TemplateRenderers {
  this: AutomaticAccessibilitySpec =>

  // TODO generate boilerplate for Template0..22[_, ...]

  def render[A, B, C, D, Result](
    template: Template4[A, B, C, D, Result]
  )(implicit a: Arbitrary[A], b: Arbitrary[B], c: Arbitrary[C], d: Arbitrary[D]): Html = {
    val maybeHtml = for {
      av <- a.arbitrary.apply(parameters, Seed.random)
      bv <- b.arbitrary.apply(parameters, Seed.random)
      cv <- c.arbitrary.apply(parameters, Seed.random)
      dv <- d.arbitrary.apply(parameters, Seed.random)
    } yield template.render(av, bv, cv, dv).asInstanceOf[Html]
    maybeHtml.get
  }

  def render[A, B, C, D, E, Result](
    template: Template5[A, B, C, D, E, Result]
  )(implicit a: Arbitrary[A], b: Arbitrary[B], c: Arbitrary[C], d: Arbitrary[D], e: Arbitrary[E]): Html = {
    val maybeHtml = for {
      av <- a.arbitrary.apply(parameters, Seed.random)
      bv <- b.arbitrary.apply(parameters, Seed.random)
      cv <- c.arbitrary.apply(parameters, Seed.random)
      dv <- d.arbitrary.apply(parameters, Seed.random)
      ev <- e.arbitrary.apply(parameters, Seed.random)
    } yield template.render(av, bv, cv, dv, ev).asInstanceOf[Html]
    maybeHtml.get
  }
}

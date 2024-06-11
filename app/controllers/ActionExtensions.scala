/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import play.api.mvc.*

import scala.concurrent.{ExecutionContext, Future}

extension (actionBuilder: ActionBuilder[Request, AnyContent]) {
  def asyncUsing(block: Request[AnyContent] ?=> Future[Result])(using ExecutionContext): Action[AnyContent] =
    actionBuilder.asyncUsingT[Request[AnyContent]](block)
}

extension (actionBuilder: ActionBuilder[Request, AnyContent]) {
  def asyncUsingT[T <: Request[_]](block: T ?=> Future[Result])(using ExecutionContext): Action[AnyContent] =
    actionBuilder.async { request =>
      given T = request.asInstanceOf[T]
      block
    }
}

/**
 *  Copyright 2013 Robert Welin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooo.nilewapps.bokbytarappen.server.authentication

import scala.concurrent.{ExecutionContext, Future}
import spray._
import routing._
import authentication._
import http._
import httpx.unmarshalling._

class SimpleTokenAuthenticator[U](
    val realm: String,
    val authenticator: Option[String] => Future[Option[U]],
    val token: Option[String] = None)
    (implicit val executionContext: ExecutionContext)
  extends ContextAuthenticator[U] {

  def apply(ctx: RequestContext) = {
    authenticate(ctx) map {
      case Some(t) => Right(t)
      case None => Left {
        AuthenticationFailedRejection(realm)
      }
    }
  }

  /**
   * Extracts a password reset token from the request entity and
   * passes it to the authenticator.
   */
  def authenticate(ctx: RequestContext) = authenticator {
    token match {
      case Some(t) => Some(t)
      case None => ctx.request.entity.as[FormData] match {
        case Right(m) => m.fields.get("token")
        case _ => None
      }
    }
  }
}

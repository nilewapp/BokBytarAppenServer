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

import spray.http._
import spray.http.HttpHeaders._
import spray.httpx.unmarshalling._
import spray.routing._
import spray.routing.authentication._

class SimpleTokenAuthenticator[U](
    val realm: String,
    val authenticator: Option[String] => Future[Option[U]],
    val fieldName: String = "token")
    (implicit val executionContext: ExecutionContext)
  extends ContextAuthenticator[U] {

  def apply(ctx: RequestContext) = {
    authenticate(ctx) map {
      case Some(t) => Right(t)
      case None =>
        Left(AuthenticationFailedRejection(
          AuthenticationFailedRejection.CredentialsRejected,
          getChallengeHeaders(ctx.request)))
    }
  }

  /**
   * Extracts a password reset token from the request entity and
   * passes it to the authenticator.
   */
  def authenticate(ctx: RequestContext) = authenticator {
    ctx.request.entity.as[FormData] match {
      case Right(m) => Map(m.fields: _*).get(fieldName)
      case _ => None
    }
  }

  def getChallengeHeaders(httpRequest: HttpRequest) =
    `WWW-Authenticate`(HttpChallenge(
      scheme = "Nilewapp", realm = realm, params = Map.empty)) :: Nil
}

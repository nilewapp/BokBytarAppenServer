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
import scala.util.parsing.combinator._

import spray._
import routing._
import authentication._
import http._
import spray.util._
import HttpHeaders._

import com.mooo.nilewapps.bokbytarappen.server.data._

/**
 * Defines fuctions to extract a Token from an `Authorization` header with
 * the format `Nilewapp key="value",...` to a Map.
 */
class NilewappTokenAuthenticator[U](
    val realm: String,
    val authenticator: Option[Token] => Future[Option[U]])
    (implicit val executionContext: ExecutionContext)
  extends ContextAuthenticator[U] {

  def apply(ctx: RequestContext) = {
    val authHeader = ctx.request.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(creds) => creds }
    authenticate(credentials) map {
      case Some(token) => Right(token)
      case None => Left {
        AuthenticationFailedRejection(realm)
      }
    }
  }

  def scheme = "Nilewapp"

  /**
   * Extracts the token from the Authorization header and passes it as
   * the argument to the authenticator.
   */
  def authenticate(credentials: Option[HttpCredentials]) = {
    authenticator {
      credentials.flatMap {
        case GenericHttpCredentials(scheme, params) => 
          val de = new sun.misc.BASE64Decoder()
          val t = params.mapValues(v => new String(de.decodeBuffer(v)))
          for {
            email <- t.get("email")
            series <- t.get("series")
            token <- t.get("token")
          } yield Token(email, series, token, None)
        case _ => None
      }
    }
  }
}

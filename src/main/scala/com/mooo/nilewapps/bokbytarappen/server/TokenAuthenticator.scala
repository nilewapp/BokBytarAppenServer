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
package com.mooo.nilewapps.bokbytarappen.server

import scala.concurrent.{ExecutionContext, Future}
import spray._
import routing._
import authentication._
import http._
import httpx._
import unmarshalling._

object BasicTokenAuthenticator {
  case class Token(user: String, series: String, token: String)
  object Token {
    def apply(vals: Map[String, String]): Option[Token] = 
      try {
        Some(Token(vals("email"), vals("series"), vals("token")))
      } catch {
        case _: NoSuchElementException => None
      }
  }
  type TokenAuthenticator[U] = Option[Token] => Future[Option[U]]
}

import BasicTokenAuthenticator._

class BasicTokenAuthenticator[U](val realm: String, val authenticator: TokenAuthenticator[U])(implicit val executionContext: ExecutionContext)
    extends ContextAuthenticator[U] {
      
  def apply(ctx: RequestContext) = {
    authenticate(ctx) map {
      case Some(token) => Right(token)
      case None => Left {
        AuthenticationFailedRejection(realm)
      }
    }
  }

  def scheme = "Basic"
  def params(ctx: RequestContext): Map[String, String] = Map.empty
  def authenticate(ctx: RequestContext) = {
    authenticator {
      ctx.request.entity.as[FormData] match {
        case Right(FormData(formFields)) => Token(formFields)
        case _ => None
      }
    }
  }
}


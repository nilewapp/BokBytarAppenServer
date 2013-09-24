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

import org.specs2.time.NoTimeConversions
import org.specs2.mutable.{Specification, BeforeAfter}

import scala.concurrent._
import duration._
import ExecutionContext.Implicits.global

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server
import server.DB._
import server.data._
import server.util._

class EmailConfirmationTokenAuthenticatorSpec
  extends Specification
  with EmailConfirmationTokenAuthenticator
  with NoTimeConversions {

  "EmailConfirmationTokenAuthenticator" should {
    "not return a valid token if no token string is given" in {
      Await.result(emailConfirmationTokenAuthenticator(None), 10 seconds) must_== None
    }
    "not return a valid token if a non existing token string is given" in {
      Await.result(emailConfirmationTokenAuthenticator(Some("")), 10 seconds) must_== None
    }
    "not return a valid token if an expired token string is given" in new ExpiredTokenContext {
      Await.result(emailConfirmationTokenAuthenticator(Some(tokenString)), 10 seconds) must_== None
    }
    "return a valid token if a valid, unexpired token string is given" in new ValidTokenContext {
      Await.result(emailConfirmationTokenAuthenticator(Some(tokenString)), 10 seconds) must_==
        Some(unexpiredToken(profileId, tokenString))
    }
  }

  def token(id: Int, s: String, t: Long) = EmailConfirmationToken(id, SHA256(s), "", t)

  def expiredToken(id: Int, s: String) = token(id, s, Long.MinValue)

  def unexpiredToken(id: Int, s: String) = token(id, s, Long.MaxValue)

  def updateToken(t: EmailConfirmationToken) = query {
    Query(EmailConfirmationTokens).filter(_.id === t.id).update(t) match {
      case 1 => 1
      case 0 => EmailConfirmationTokens.insert(t)
    }
  }

  trait Context {

    var profileId: Int = -1

    val tokenString = SecureString()
  }

  trait ExpiredTokenContext extends BeforeAfter with Context {

    def before = {
      query {
        profileId = insertProfile("", "", None)
        lazy val t = expiredToken(profileId, tokenString)
        updateToken(t)
      }
    }

    def after = {
      query {
        Query(EmailConfirmationTokens).filter(_.id === profileId).delete
        Query(Profiles).filter(_.id === profileId).delete
      }
    }

  }

  trait ValidTokenContext extends BeforeAfter with Context {

    def before = {
      query {
        profileId = insertProfile("", "", None)
        lazy val t = unexpiredToken(profileId, tokenString)
        updateToken(t)
      }
    }

    def after = {
      query {
        Query(EmailConfirmationTokens).filter(_.id === profileId).delete
        Query(Profiles).filter(_.id === profileId).delete
      }
    }
  }
}

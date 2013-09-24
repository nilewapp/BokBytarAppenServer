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

class PasswordResetTokenAuthenticatorSpec
  extends Specification
  with PasswordResetTokenAuthenticator
  with NoTimeConversions {

  "PasswordResetTokenAuthenticatorSpec" should {
    "return None if no token is given" in {
      Await.result(passwordResetTokenAuthenticator(None), 10 seconds) must_== None
    }
    "return None if a non existing token is given" in {
      Await.result(passwordResetTokenAuthenticator(Some("")), 10 seconds) must_== None
    }
    "return None if an expired token is given" in new ExpiredTokenContext {
      Await.result(passwordResetTokenAuthenticator(Some(tokenString)), 10 seconds) must_== None
    }
    "return the Profile the token belongs if the token is valid" in new ValidTokenContext {
      Await.result(passwordResetTokenAuthenticator(Some(tokenString)), 10 seconds) must_==
        Some(Profile(profileId, None, "", "", None))
    }
  }

  trait Context {
    var profileId: Int = -1
    var tokenString: String = ""
  }

  trait ExpiredTokenContext extends Context with BeforeAfter {

    def before = query {
      profileId = insertProfile("", "", None)
      tokenString = SecureString()
      PasswordResetTokens.insert(SimpleToken(profileId, SHA256(tokenString), Long.MinValue))
    }

    def after = query {
      Query(PasswordResetTokens).filter(_.id === profileId).delete
      Query(Profiles).filter(_.id === profileId).delete
    }
  }

  trait ValidTokenContext extends Context with BeforeAfter {

    def before = query {
      profileId = insertProfile("", "", None)
      tokenString = SecureString()
      PasswordResetTokens.insert(SimpleToken(profileId, SHA256(tokenString), Long.MaxValue))
    }

    def after = query {
      Query(PasswordResetTokens).filter(_.id === profileId).delete
      Query(Profiles).filter(_.id === profileId).delete
    }
  }
}

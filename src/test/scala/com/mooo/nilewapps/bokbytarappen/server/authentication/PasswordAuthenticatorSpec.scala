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

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import org.specs2.time.NoTimeConversions
import org.specs2.mutable.{NameSpace, Specification, BeforeAfter}
import spray.routing.authentication.UserPass

import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.data.{Profile, Token, Session => S}
import com.mooo.nilewapps.bokbytarappen.server.util._

class PasswordAuthenticatorSpec
  extends Specification
  with PasswordAuthenticator
  with NoTimeConversions {

  "PasswordAuthenticator No Session" should {
    "return None if no credentials are given" in {
      Await.result(passwordAuthenticatorNoSession(None), 10 seconds) must_== None
    }
    "return None if credentials for a non registered user are given" in new NoSessionContext {
      val credentials = Some(UserPass(SecureString(), pass))
      Await.result(passwordAuthenticatorNoSession(credentials), 10 seconds) must_== None
    }
    "return None if an incorrect password is given" in new NoSessionContext {
      val credentials = Some(UserPass(user, SecureString()))
      Await.result(passwordAuthenticatorNoSession(credentials), 10 seconds) must_== None
    }
    "return the result of the given method of an authenticated user" in new NoSessionContext {
      val credentials = Some(UserPass(user, pass))
      Await.result(passwordAuthenticatorNoSession(credentials), 10 seconds) must_==
        Some(Profile(profileId, Some(user), passHash, "", None))
    }
  }

  "PasswordAuthenticator New Session" should {
    "return a Profile and a new session Token if the credentials are correct" in new NewSessionContext {
      Await.result(passwordAuthenticator(credentials), 10 seconds) match {
        case Some((p, Token(email, _, _, _))) =>
          p must_== Profile(profileId, Some(user), passHash, "", None)
          email must_== user
        case _ => failure("A Profile and a Token was not returned")
      }
    }
    "put a new Session in the database equal to the returned Token" in new NewSessionContext {
      Await.result(passwordAuthenticator(credentials), 10 seconds) match {
        case Some((Profile(id, Some(email), _, _, _), Token(tokenEmail, series, token, expirationTime))) =>
          email must_== tokenEmail
          query {
            val seriesHash = SHA256(series)
            Query(Sessions).filter(q => q.id === id && q.seriesHash === seriesHash).list match {
              case result: List[S] =>
                result.length must_== 1
                result.head.id must_== id
                result.head.seriesHash must_== seriesHash
                result.head.tokenHash must_== SHA256(token)
                result.head.expirationTime must_== expirationTime.get
              case _ => failure("A Session was not put into the database")
            }
          }
        case _ => failure("A Profile and a Token was not returned")
      }
    }
  }

  trait NoSessionContext extends BeforeAfter {

    val user = SecureString()
    val pass = SecureString()
    val passHash = BCrypt.hashpw(pass, BCrypt.gensalt())
    var profileId: Int = -1

    def before = query {
      profileId = (Profiles.email ~
       Profiles.passwordHash ~
       Profiles.name ~
       Profiles.phoneNumber) returning Profiles.id insert((
        Some(user), passHash, "", None))
    }

    def after = query {
      Query(Profiles).filter(_.email === user).delete
    }
  }

  trait NewSessionContext extends NoSessionContext {

    val credentials = Some(UserPass(user, pass))

    override def after = query {
      Query(Sessions).filter(_.id === profileId).delete
      super.after
    }
  }
}

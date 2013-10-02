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

import org.specs2.time.NoTimeConversions
import org.specs2.mutable.{Specification, BeforeAfter}
import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.data.{Token, Profile, Session => S}
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.util._

class SessionAuthenticatorSpec
  extends Specification
  with SessionAuthenticator
  with NoTimeConversions {

  def auth(c: Option[Token]) =
    sessionAuthenticator(c, (p, t) => Some((p, t)))

  "SessionAuthenticator" should {
    "return None if no token is given" in {
      Await.result(auth(None), 10 seconds) must_== None
    }
    "return None if a token belonging to an unregistered email address is given" in {
      Await.result(
        auth(Some(Token("", "", "", None))), 10 seconds) must_== None
    }
    "return the result of the method parameter if a token belonging to a registered user is given" in
      new ProfileContext {
      Await.result(
        auth(Some(Token(email, "", "", None))), 10 seconds) must_==
          Some((Profile(profileId, Some(email), "", "", None),
            Token(email, "", "", None)))
    }
  }
  "SessionAuthenticator No Session" should {
    "return None if an invalid series is given" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticatorNoSession(
          Some(Token(email, "", token, None))), 10 seconds) must_== None
    }
    "return None if an invalid token is given" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticatorNoSession(
          Some(Token(email, series, "", None))), 10 seconds) must_== None
    }
    "return None if an expired token is given" in
      new ExpiredSessionContext {
      Await.result(
        sessionAuthenticatorNoSession(
          Some(Token(email, series, token, None))), 10 seconds) must_== None
    }
    "return a profile if the token is authenticated" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticatorNoSession(
          Some(Token(email, series, token, None))), 10 seconds) must_==
            Some(Profile(profileId, Some(email), "", "", None))
    }
    "delete the session after authentication" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticatorNoSession(
          Some(Token(email, series, token, None))), 10 seconds)
      query {
        Query(Sessions).filter(q =>
          q.id === profileId && q.seriesHash === SHA256(series)).list must be empty
      }
    }
  }
  "SessionAuthenticator New Session" should {
    "return None if an invalid series is given" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticator(
          Some(Token(email, "", token, None))), 10 seconds) must_== None
    }
    "return None if an invalid token is given" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticator(
          Some(Token(email, series, "", None))), 10 seconds) must_== None
    }
    "return None if an expired token is given" in
      new ExpiredSessionContext {
      Await.result(
        sessionAuthenticator(
          Some(Token(email, series, token, None))), 10 seconds) must_== None
    }
    "return a profile and a new token if the token is authenticated" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticator(
          Some(Token(email, series, token, None))), 10 seconds) match {
        case Some((p: Profile, t: Token)) =>
          p must_== Profile(profileId, Some(email), "", "", None)
          t.email must_== email
          t.series must_== series
          t.token must_!= token
        case _ => failure("A Profile/Token-pair wasn't returned")
      }
    }
    "update the session after authentication" in
      new UnexpiredSessionContext {
      Await.result(
        sessionAuthenticator(
          Some(Token(email, series, token, None))), 10 seconds)
      query {
        val sessions =
          Query(Sessions).filter(q =>
            q.id === profileId && q.seriesHash === SHA256(series)).list

        sessions must have size (1)
        sessions.head.tokenHash must_!= SHA256(token)
      }
    }
  }

  trait ProfileContext extends BeforeAfter {

    var profileId: Int = -1
    val email = SecureString()

    def before = query {
      profileId = (Profiles.email ~
       Profiles.passwordHash ~
       Profiles.name ~
       Profiles.phoneNumber) returning Profiles.id insert((
        Some(email), "", "", None))
    }

    def after = query {
      Query(Profiles).filter(_.id === profileId).delete
    }
  }

  trait SessionContext extends ProfileContext {

    val series = SecureString()
    val token = SecureString()
    def expirationTime: Long

    override def before = query {
      super.before
      Sessions.insert(
        S(profileId, SHA256(series), SHA256(token), expirationTime))
    }

    override def after = query {
      Query(Sessions).filter(q =>
        q.id === profileId && q.seriesHash === SHA256(series)).delete
      super.after
    }
  }

  trait ExpiredSessionContext extends SessionContext {
    def expirationTime = Long.MinValue
  }

  trait UnexpiredSessionContext extends SessionContext {
    def expirationTime = Long.MaxValue
  }
}

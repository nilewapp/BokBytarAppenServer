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
import ExecutionContext.Implicits.global
import spray.routing.authentication.UserPass

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.typesafe.config._

import com.mooo.nilewapps.bokbytarappen.server
import server.data._
import server.util._
import server.DB._

trait PasswordAuthenticator {

  /**
   * Authenticate a user with password and execute a method on the profile.
   */
  def passwordAuthenticator[U](
      credentials: Option[UserPass],
      f: Profile => Option[U]): Future[Option[U]] = future {
    (for {
      c <- credentials
      profile <- query(getProfile(c.user))
      if BCrypt.checkpw(c.pass, profile.passwordHash)
    } yield profile) flatMap f
  }

  /**
   * Takes a user/pass-pair, checks their validity and returns the
   * profile of the user and a new Session.
   */
  def passwordAuthenticator(credentials: Option[UserPass]): Future[Option[(Profile, Token)]] =
    passwordAuthenticator(credentials, p => {
      lazy val series = SecureString()
      lazy val token = SecureString()
      lazy val time = System.currentTimeMillis() + ConfigFactory.load().getMilliseconds("session.expiration-time")
      if (query(Sessions.insert(Session(p.id, SHA256(series), SHA256(token), time))) == 1) {
        Some(p, Token(p.email.get, series, token, Some(time)))
      } else None
    })

  /**
   * Takes a user/pass-pair, checks their validity and returns the
   * profile of the user without creating a new Session.
   */
  def passwordAuthenticatorNoSession(credentials: Option[UserPass]): Future[Option[Profile]] =
    passwordAuthenticator(credentials, Some(_))

}

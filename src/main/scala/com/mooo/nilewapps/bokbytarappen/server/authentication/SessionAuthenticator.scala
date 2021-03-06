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
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config._
import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.data._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.util._

trait SessionAuthenticator {

  /**
   * Excutes a method on a token and the profile that the token belongs to.
   */
  def sessionAuthenticator[U](
      credentials: Option[Token],
      f: (Profile, Token) => Option[U]): Future[Option[U]] = future {
    query {
      (for {
        t <- credentials
        p <- getProfile(t.email)
      } yield (p, t)) flatMap {
        case (p, t) => f(p, t)
      }
    }
  }

  /**
   * Takes a token, checks its validity, returns the profile the token belongs to and a new Session.
   */
  def sessionAuthenticator(
      credentials: Option[Token]): Future[Option[(Profile, Token)]] = {
    sessionAuthenticator(credentials, (p, t) => {

      lazy val token = SecureString()

      lazy val currentTime = System.currentTimeMillis()

      lazy val expires = currentTime +
        ConfigFactory.load().getMilliseconds("session.expiration-time")

      lazy val seriesHash = SHA256(t.series)

      (for {
        s <- Sessions
        if s.id === p.id &&
           s.seriesHash === seriesHash &&
           s.tokenHash === SHA256(t.token) &&
           s.expirationTime > currentTime
      } yield s).update(Session(p.id, seriesHash, SHA256(token), expires)) match {
        case 1 => Some(p, Token(t.email, t.series, token, Some(expires)))
        case 0 => None
      }
    })
  }

  /**
   * Takes a token, checks its validity, deletes the token and returns
   * the profile the token belongs to.
   */
  def sessionAuthenticatorNoSession(
      credentials: Option[Token]): Future[Option[Profile]] = {
    sessionAuthenticator(credentials, (p, t) => {
      (for {
        s <- Sessions
        if s.id === p.id &&
           s.seriesHash === SHA256(t.series) &&
           s.tokenHash === SHA256(t.token) &&
           s.expirationTime > System.currentTimeMillis()
      } yield s).delete match {
        case 1 => Some(p)
        case 0 => None
      }
    })
  }
}

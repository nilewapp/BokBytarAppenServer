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

import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.routing.authentication.UserPass

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.typesafe.config._

import DB._

trait Authenticator {

  /**
   * The duration in ms for which the session token is valid.
   */
  lazy val expirationTime = ConfigFactory.load().getMilliseconds("session.expiration-time")

  /**
   * Authenticate a user with password and execute a method on the profile.
   */
  def passwordAuthenticator[U](
      credentials: Option[UserPass],
      f: Profile => Option[U]): Future[Option[U]] = future {
    credentials match {
      case Some(c) =>
        query {
          getProfile(c.user) match {
            case Some(profile) =>
              if (BCrypt.checkpw(c.pass, profile.passwordHash)) f(profile)
              else None
            case _ => None
          }
        }
      case _ => None
    }
  }

  /**
   * Takes a user/pass-pair, checks their validity and returns the
   * profile of the user and a new Session.
   */
  def passwordAuthenticator(credentials: Option[UserPass]): Future[Option[(Profile, Token)]] =
    passwordAuthenticator(credentials, p => {
      lazy val series = SecureString()
      lazy val token = SecureString()
      lazy val time = System.currentTimeMillis() + expirationTime
      if (Sessions.insert(Session(p.id, SHA256(series), SHA256(token), time)) == 1) {
        Some(p, Token(p.email.get, series, token, Some(time)))
      } else None
    })

  /**
   * Takes a user/pass-pair, checks their validity and returns the
   * profile of the user without creating a new Session.
   */
  def passwordAuthenticatorNoSession(credentials: Option[UserPass]): Future[Option[Profile]] =
    passwordAuthenticator(credentials, Some(_))


  /**
   * Excutes a method on a token and the profile that the token belongs to.
   */
  def tokenAuthenticator[U](
      credentials: Option[Token],
      f: (Profile, Token) => Option[U]): Future[Option[U]] = future {
    credentials match {
      case Some(t) => query {
        getProfile(t.email) match {
          case Some(p) => f(p, t)
          case _ => None
        }
      }
      case _ => None
    }
  }

  /**
   * Takes a token, checks its validity, returns the profile the token belongs to and a new Session.
   */
  def tokenAuthenticator(credentials: Option[Token]): Future[Option[(Profile, Token)]] = {
    tokenAuthenticator(credentials, (p, t) => {
      lazy val token = SecureString()
      lazy val currentTime = System.currentTimeMillis()
      lazy val expires = currentTime + expirationTime
      lazy val seriesHash = SHA256(t.series)
      (for {
        s <- Sessions
        if s.id        === p.id      &&
           s.series    === seriesHash      &&
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
  def tokenAuthenticatorNoSession(credentials: Option[Token]): Future[Option[Profile]] = {
    tokenAuthenticator(credentials, (p, t) => {
      (for {
        s <- Sessions
        if s.id        === p.id       &&
           s.series    === SHA256(t.series) &&
           s.tokenHash === SHA256(t.token)  &&
           s.expirationTime > System.currentTimeMillis()
      } yield s).delete match {
        case 1 => Some(p)
        case 0 => None
      }
    })
  }

  /**
   * Takes a password reset token and returns the profile it belongs to if it is valid.
   */
  def passwordResetTokenAuthenticator(token: Option[String]): Future[Option[Profile]] = future {
    token match {
      case Some(t) => query {
        (for {
          prt <- PasswordResetTokens
          profile <- Profiles
          if prt.token === SHA256(t)  &&
             prt.id    === profile.id &&
             prt.expirationTime > System.currentTimeMillis()
        } yield profile).list.headOption
      }
      case _ => None
    }
  }
}

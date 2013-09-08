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

import java.security.SecureRandom
import java.security.MessageDigest
import java.math.BigInteger
import sun.misc.BASE64Encoder

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import BasicTokenAuthenticator._

import com.typesafe.config._

trait Authenticator extends DB {

  /**
   * The duration in ms for which the session token is valid.
   */
  lazy val expirationTime = ConfigFactory.load().getMilliseconds("session.expiration-time")

  /**
   * Used for hashing authentication tokens before storing them in the database.
   */
  lazy val sha256 = MessageDigest.getInstance("SHA-256")

  /**
   * Takes a user/pass-pair, check their validity and returns the profile of the user and a new Session.
   */
  def passwordAuthenticator(credentials: Option[UserPass]): Future[Option[(Profile, Token)]] = future {
    credentials match {
      case Some(c) =>
        query { 
          Query(Profiles).filter(_.email === c.user).list.headOption match {
            case Some(profile) =>
              if (BCrypt.checkpw(c.pass, profile.passwordHash)) {
                lazy val series = generateSecureString()
                lazy val token = generateSecureString()
                lazy val time = System.currentTimeMillis() + expirationTime
                if (Sessions.insert(Session(profile.id, hash(series), hash(token), time)) == 1)
                  Some(profile, Token(profile.email, series, token, Some(time)))
                else 
                  None
              } else 
                None
            case _ => None
          }
        }
      case _ => None
    }
  }

  /**
   * Takes a token, checks its validity, returns the profile the token belongs to and a new Session.
   */
  def tokenAuthenticator(credentials: Option[Token]): Future[Option[(Profile, Token)]] = future {
    credentials match {
      case Some(t) => query { 
        Query(Profiles).filter(_.email === t.email).list.headOption match {
          case Some(profile) =>
            lazy val token = generateSecureString()
            lazy val time = System.currentTimeMillis()
            lazy val expires = time + expirationTime
            lazy val seriesHash = hash(t.series)
            (for {
              s <- Sessions 
              if s.id        === profile.id    &&
                 s.series    === seriesHash    &&
                 s.tokenHash === hash(t.token) &&
                 s.expirationTime > System.currentTimeMillis()
            } yield s).update(Session(profile.id, seriesHash,  hash(token), expires)) match {
              case 1 => 
                println("Generated token: " + Token(t.email, t.series, token, Some(expires)))
                Some(profile, Token(t.email, t.series, token, Some(expires)))
              case 0 => 
                println("Failed to generate new token")
                None
            }
          case _ => None
        }
      }
      case _ => println("No token"); None
    }
  }

  def generateSecureString() = {
    val sr = new SecureRandom
    val token = new BigInteger(130, sr).toString(64).getBytes
    new String(new BASE64Encoder().encode(token))
  }

  def hash(s: String) =
    new String(sha256.digest(s.getBytes))

}

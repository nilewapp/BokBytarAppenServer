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

import java.security.SecureRandom
import java.math.BigInteger
import sun.misc.BASE64Encoder

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.typesafe.config._

object SessionFactory extends DB {

  /**
   * The duration in ms for which the session token is valid.
   */
  lazy val expirationTime = ConfigFactory.load().getMilliseconds("session.expiration-time")

  /**
   * Validates the session, produces a new one and updates the database.
   */
  def apply(s: Session) = {
    println(expirationTime)
    query { 
      lazy val time = System.currentTimeMillis()
      lazy val newSession =
        Session(s.profile, s.series, generateSecureString(), time + expirationTime)
      (for {
        session <- Sessions
        if session.profile === s.profile &&
           session.series  === s.series  &&
           session.token   === s.token   &&
           session.expirationTime > time
      } yield session).update(newSession) match {
        case 1 => Some(newSession)
        case _ => None
      }
    }
  }

  /**
   * Generates a new session token for an existing user and stores in in the database.
   */
  def apply(user: String) = {
    query {
      if (!Query(Profiles).filter(_.id === user).list.isEmpty) {
        lazy val session = Session(user,
          generateSecureString(),
          generateSecureString(),
          System.currentTimeMillis() + expirationTime)
        if (Sessions.insert(session) == 1) Some(session)
        else None
      } else {
        None
      }
    }
  }

  def generateSecureString() = {
    val sr = new SecureRandom
    val token = new BigInteger(130, sr).toString(64).getBytes
    new String(new BASE64Encoder().encode(token))
  }
}

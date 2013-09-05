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

import BasicTokenAuthenticator._

trait Authenticator extends DB {

  /**
   * Takes a user/pass-pair, check their validity and returns the profile of the user and a new Session.
   */
  def passwordAuthenticator(credentials: Option[UserPass]): Future[Option[(Profile, Session)]] = future {
    credentials match {
      case Some(c) =>
        query { Query(Profiles).filter(_.id === c.user).list.headOption } match {
          case Some(user) =>
            if (BCrypt.checkpw(c.pass, user.passwordHash)) SessionFactory(user.id) match {
              case Some(session) => Some((user, session))
              case _ => None
            }
            else None
          case _ => None
        }
      case _ => None
    }
  }

  /**
   * Takes a token, checks its validity, returns the profile the token belongs to and a new Session.
   */
  def tokenAuthenticator(credentials: Option[Token]): Future[Option[(Profile, Session)]] = future {
    credentials match {
      case Some(t) => query { 
        (for {
          p <- Profiles if p.id === t.profile
          s <- Sessions 
          if p.id     === s.profile &&
             s.series === t.series &&
             s.token  === t.token  &&
             s.expirationTime > System.currentTimeMillis()
        } yield (p, s)).take(1).list.headOption.flatMap {
          a => SessionFactory(a._2) match {
            case Some(session) => println("Generated token: " + session); Some(a._1, session)
            case _ => println("Failed to generate new token"); None
          }
        }
      }
      case _ => println("No token"); None
    }
  }
}

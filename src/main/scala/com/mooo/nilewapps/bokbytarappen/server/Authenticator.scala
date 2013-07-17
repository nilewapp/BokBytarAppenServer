/*
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

import sun.misc.BASE64Decoder

import BasicTokenAuthenticator._

trait Authenticator extends DB {

  def passwordAuthenticator(credentials: Option[UserPass]): Future[Option[Profile]] = future {
    credentials match {
      case Some(c) =>
        query { Query(Profiles).filter(_.id === c.user).take(1).list.headOption } match {
          case Some(user) =>
            if (BCrypt.checkpw(c.pass, user.passwordHash)) Some(user)
            else None
          case _ => None
        }
      case _ => None
    }
  }

  def tokenAuthenticator(credentials: Option[Token]): Future[Option[Profile]] = future {
    credentials match {
      case Some(t) => query { 
        (for {
          profile <- Profiles if profile.id === t.user
          session <- Sessions 
          if profile.id     === session.profile &&
             session.series === t.series &&
             session.token  === t.token  &&
             session.expirationTime > System.currentTimeMillis()
        } yield profile).take(1).list.headOption
      }
      case _ => None
    }
  }
}

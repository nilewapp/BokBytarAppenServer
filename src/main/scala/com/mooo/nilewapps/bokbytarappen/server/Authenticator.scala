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

trait Authenticator extends DB {

  def checkpw(password: String, hash: String) = BCrypt.checkpw(password, hash)

  def authenticator(credentials: Option[UserPass]): Future[Option[Profile]] = future {
    val de = new BASE64Decoder()
    def decode(s: String) = new String(de.decodeBuffer(s))
    val decoded = credentials.map(up => 
      UserPass(decode(up.user), decode(up.pass))).get
    val user = Query(Profiles).filter(_.id === decoded.user).take(1).list.head
    if (checkpw(decoded.pass, user.passwordHash)) {
      Some(user)
    } else {
      None
    }
  }
}

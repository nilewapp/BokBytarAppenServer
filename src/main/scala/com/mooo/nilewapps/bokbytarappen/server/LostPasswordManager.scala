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

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.typesafe.config._

object LostPasswordManager extends DB {

  /**
   * Creates a password reset token and stores it in the database if
   * the given email address is registered.
   */
  def requestResetToken(email: String): Option[String] = {

    lazy val expirationTime = ConfigFactory.load().getMilliseconds("password-reset.expiration-time")

    query {
      Query(Profiles).filter(_.email === email).list.headOption match {
        case Some(profile) =>
          lazy val token = SecureString()
          lazy val expires = System.currentTimeMillis() + expirationTime
          PasswordResetTokens.insert(PasswordResetToken(profile.id, SHA256(token), expires)) match {
            case 1 => Some(token)
            case _ => None
          }
        case _ => None
      }
    }
  }

  /**
   * Creates a password reset token and sends a reset link to the given
   * email address if it is registered in the database.
   */
  def sendResetLink(email: String) = LostPasswordManager.requestResetToken(email) match {
    case Some(token) => MailAgent.send(
      email,
      "Password reset link",
      "Hello,\n\nTo reset your password, please click the link below:\n\n" +
      ConfigFactory.load().getString("http-server.domain") +
      "/change-password/" + token + "\n\nMany thanks,\nRobert")
    case _ =>
  }
}

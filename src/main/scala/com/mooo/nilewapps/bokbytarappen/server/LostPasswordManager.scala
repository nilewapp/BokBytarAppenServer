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

import DB._
import util._

import data._

object LostPasswordManager {

  /**
   * Creates a password reset token and stores it in the database if
   * the given email address is registered. Replaces any old token.
   */
  def requestResetToken(email: String): Option[String] = query {
    
    lazy val tokenString = SecureString()

    def expirationTime = System.currentTimeMillis() +
      ConfigFactory.load().getMilliseconds("password-reset.expiration-time")

    for {
      profile <- getProfile(email)

      token = SimpleToken(profile.id, SHA256(tokenString), expirationTime)

      if Query(PasswordResetTokens).filter(q => q.id === profile.id).update(token) == 1 ||
         PasswordResetTokens.insert(token) == 1

    } yield tokenString
  }

  /**
   * Creates a password reset token and sends a reset link to the given
   * email address if it is registered in the database.
   */
  def sendResetLink(email: String) = requestResetToken(email) match {
    case Some(token) => MailAgent.send(
      email,
      "Password reset link",
      "Hello,\n\nTo reset your password, please click the link below:\n\n" +
      ConfigFactory.load().getString("http-server.domain") +
      "/change-password/" + token + "\n\nMany thanks,\nRobert")
    case _ =>
  }
}

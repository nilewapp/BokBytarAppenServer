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

import com.typesafe.config._
import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.data.EmailConfirmationToken
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.util._

object EmailChangeManager {

  /**
   * Updates the email of the profile and returns the new email address.
   */
  def confirmEmail(token: EmailConfirmationToken): Option[String] = query {
    for {
      profile <- getProfile(token.id)
      if profile.updateEmail(Some(token.email)) == 1
    } yield token.email
  }

  /**
   * Creates an email confirmation token limited to one per account.
   */
  def requestEmailConfirmationToken(
      id: Int,
      email: String): Option[String] = query {


    def expirationTime = System.currentTimeMillis() +
      ConfigFactory.load().getMilliseconds("email-confirmation.expiration-time")

    for {
      profile <- getProfile(id)

      tokenString = SecureString()
      token = EmailConfirmationToken(
        profile.id, SHA256(tokenString), email, expirationTime)

      if Query(EmailConfirmationTokens)
          .filter(_.id === profile.id).update(token) == 1 ||
         EmailConfirmationTokens.insert(token) == 1

    } yield tokenString
  }

  /**
   * Creates an email confirmation token and sends to the given address
   * for confirmation.
   */
  def requestEmailChange(id: Int, email: String) = {
    requestEmailConfirmationToken(id, email) match {
      case Some(token) => MailAgent.send(
        email,
        "Email confirmation link",
        "Hello,\n\nTo confirm your email address and complete your registration, " +
        "please click the link below:\n\n" +
        ConfigFactory.load().getString("http-server.domain") +
        "/confirm-email/" + token + "\n\nMany thanks,\nRobert")
      case _ =>
    }
  }
}

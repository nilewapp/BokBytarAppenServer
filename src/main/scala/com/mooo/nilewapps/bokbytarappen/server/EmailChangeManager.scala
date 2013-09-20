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

import data.EmailConfirmationToken

object EmailChangeManager {

  /**
   * Updates the email of the profile and returns the new email address.
   */
  def confirmEmail(token: EmailConfirmationToken): Option[String] = query {
    getProfile(token.id) match {
      case Some(profile) => updateEmail(profile, Some(token.email)) match {
        case 1 => Some(token.email)
        case 0 => None
      }
      case _ => None
    }
  }

  /**
   * Creates an email confirmation token limited to one per account.
   */
  def requestEmailConfirmationToken(id: Int, email: String): Option[String] = query {
    getProfile(id) match {
      case Some(profile) =>
        lazy val token = SecureString()
        lazy val confToken = EmailConfirmationToken(profile.id, SHA256(token), email)
        Query(EmailConfirmationTokens).filter(_.id === profile.id).update(confToken) match {
          case 1 => Some(token)
          case 0 => EmailConfirmationTokens.insert(
            EmailConfirmationToken(profile.id, SHA256(token), email)) match {
              case 1 => Some(token)
              case 0 => None
            }
        }
      case _ => None
    }
  }

  /**
   * Creates an email confirmation token and sends to the given address for confirmation.
   */
  def requestEmailChange(id: Int, email: String) = requestEmailConfirmationToken(id, email) match {
    case Some(token) => MailAgent.send(
      email,
      "Email confirmation link",
      "Hello,\n\nTo confirm your email address and complete your registration, please click the link below:\n\n" +
      ConfigFactory.load().getString("http-server.domain") +
      "/confirm-email/" + token + "\n\nMany thanks,\nRobert")
    case _ =>
  }
}

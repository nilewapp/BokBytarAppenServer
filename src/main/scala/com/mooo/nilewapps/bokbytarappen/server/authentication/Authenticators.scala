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
package com.mooo.nilewapps.bokbytarappen.server.authentication

import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.routing.authentication.UserPass

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.typesafe.config._

import com.mooo.nilewapps.bokbytarappen.server
import server.data._
import server.util._
import server.DB._

trait Authenticators extends PasswordAuthenticators
  with TokenAuthenticators
  with PasswordResetTokenAuthenticator
  with EmailConfirmationTokenAuthenticator

/**
 * Contains a few default authenticators.
 */
object Authenticators extends Authenticators {

  import spray.routing.Directives.authenticate
  import spray.routing.authentication.BasicHttpAuthenticator

  /**
   * Authenticates with a user/pass-pair and returns the user Profile and a new session Token.
   */
  def authWithPass = authenticate(new BasicHttpAuthenticator("Protected", passwordAuthenticator))

  /**
   * Authenticates with a user/pass-pair and returns the user Profile.
   */
  def authWithPassNoSession =
    authenticate(new BasicHttpAuthenticator("Protected", passwordAuthenticatorNoSession))

  /**
   * Authenticates with a session Token and returns the user Profile and a new session Token.
   */
  def authWithToken = authenticate(new NilewappTokenAuthenticator("Protected", tokenAuthenticator))

  /**
   * Authenticates with a session Token and returns the user Profile.
   */
  def authWithTokenNoSession =
    authenticate(new NilewappTokenAuthenticator("Protected", tokenAuthenticatorNoSession))

  /**
   * Authenticates with a password reset token.
   */
  def authWithPasswordResetToken =
    authenticate(new SimpleTokenAuthenticator("Password reset", passwordResetTokenAuthenticator))

  /**
   * Authenticates with an email confirmation token.
   */
  def authWithEmailConfirmationToken =
    authenticate(new SimpleTokenAuthenticator("Email confirmation", emailConfirmationTokenAuthenticator))

}

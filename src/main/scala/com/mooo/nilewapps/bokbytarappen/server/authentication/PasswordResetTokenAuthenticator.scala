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
import scala.concurrent.ExecutionContext.Implicits.global

import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.data._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.util._

trait PasswordResetTokenAuthenticator {

  /**
   * Takes a password reset token and returns the profile it belongs
   * to if it is valid.
   */
  def passwordResetTokenAuthenticator(
      token: Option[String]): Future[Option[Profile]] = future {
    token match {
      case Some(t) => query {
        (for {
          prt <- PasswordResetTokens
          profile <- Profiles

          if prt.token === SHA256(t) &&
            prt.id === profile.id &&
            prt.expirationTime > System.currentTimeMillis()

        } yield profile).take(1).list.headOption
      }
      case _ => None
    }
  }
}

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
package com.mooo.nilewapps.bokbytarappen.server.service

import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data.SessMess
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.LostPasswordManager
import com.mooo.nilewapps.bokbytarappen.server.validation.Validators._

/**
 * Contains routes for services regarding user password management.
 */
trait PasswordService extends HttpService {

  /**
   * Resonds with password reset form.
   */
  def passwordChangeForm(token: String) = {
    complete {
      <html>
        <body>
          <h1>Enter your new password:</h1>
          <form
              name="password-reset-form"
              action="/change-password"
              method="POST">
            <input type="password" size="25" name="password" />
            <input type="hidden" name="token" value={token} />
            <input type="submit" value="Submit" />
          </form>
        </body>
      </html>
    }
  }

  /**
   * Allows the user to change password either with normal user/pass
   * authentication or by providing a password reset token obtained
   * from a password reset email.
   */
  def changePassword = {
    (authWithPassNoSession | authWithPasswordResetToken) { case user =>
      formField('password) { password =>
        validatePassword(password) {
          complete {
            query(updatePassword(user, password))
            implicit val SessMessStringFormat = jsonFormat2(SessMess[String])
            SessMess(None, "Password successfully changed!")
          }
        }
      }
    }
  }

  /**
   * Sends a password reset link to a given email address.
   */
  def lostPassword = {
    formField('email) { email =>
      complete {
        LostPasswordManager.sendResetLink(email)
        implicit val SessMessStringFormat = jsonFormat2(SessMess[String])
        SessMess(None, "A reset link has been sent to \'" + email + "\'.")
      }
    }
  }
}

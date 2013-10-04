/**
 *  Copyright 2013 Robert Welin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooo.nilewapps.bokbytarappen.server.service

import spray.httpx.SprayJsonSupport._
import spray.routing.Directives._

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data.SessMess
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.EmailChangeManager
import com.mooo.nilewapps.bokbytarappen.server.ServiceErrors._
import com.mooo.nilewapps.bokbytarappen.server.validation.Validators._

/**
 * Contains routes for services regarding user email management.
 */
trait EmailService {

  /**
   * Responds with a page that immidiately redirects to a service that
   * will confirm an email address with the given token.
   */
  def emailConfirmationPage(token: String) = {
    complete {
      lazy val formName = "email-confirmation-form"
      lazy val formSubmit = """
        window.onload = function() {
          document.getElementById('%s').submit()
        }
      """.format(formName)

      <html>
        <body>
          <form
              name={formName}
              id={formName}
              action="/confirm-email"
              method="POST">
            <input type="hidden" name="token" value={token} />
          </form>
          <script type="text/javascript">
            {formSubmit}
          </script>
        </body>
      </html>
    }
  }

  /**
   * Lets the user change his email address.
   */
  def changeEmail = {
    authWithPassNoSession { case user =>
      formField('email) { email =>
        validateEmail(email) {
          complete {
            EmailChangeManager.requestEmailChange(user.id, email)
            implicit val SessMessStringFormat = jsonFormat2(SessMess[String])
            SessMess(None,
              "A confirmation email has been sent to %s!".format(email))
          }
        }
      }
    }
  }

  /**
   * Lets the user confirm his email address.
   */
  def confirmEmail = {
    authWithEmailConfirmationToken { token =>
      complete {
        def page(s: String) = {
          <html>
            <body>
              <h1>{s}</h1>
            </body>
          </html>
        }

        EmailChangeManager.confirmEmail(token) match {
          case Some(email) =>
            page("Your email address %s has been confirmed!".format(email))
          case None =>
            page("Your email address was not confirmed...")
        }
      }
    }
  }
}

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

import javax.net.ssl.SSLException

import akka.actor.Actor
import scala.concurrent.duration._

import spray.http._
import spray.http.StatusCodes._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.routing.authentication._
import spray.util.LoggingContext

import MediaTypes._
import spray.json.DefaultJsonProtocol
import SprayJsonSupport._

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import ServiceErrors._

import scala.language.postfixOps

import com.typesafe.config._

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data._
import com.mooo.nilewapps.bokbytarappen.server.data.GroupPrivacy._
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.util._
import com.mooo.nilewapps.bokbytarappen.server.validation._

/**
 * Actor that runs the service.
 */
class ServiceActor extends Actor with Service {

  def actorRefFactory = context

  def receive = runRoute(routes)

}

/**
 * Provides all functionality of the server.
 */
trait Service extends HttpService {

  /**
   * Asserts that an email address is valid and available.
   */
  def validateEmail(email: String) =
    validate(EmailValidator.isValid(email), InvalidEmail) &
    validate(EmailValidator.isAvailable(email), UnavailableEmail)

  /**
   * Asserts that a password has sufficient guessing entropy.
   */
  def validatePassword(password: String) =
    validate(PasswordValidator.threshold(password), BadPassword)

  implicit val SessMessStringFormat = jsonFormat2(SessMess[String])

  val routes =
    get {
      path("") {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Say hello to <i>Bokbytarappen's server</i>!</h1>
              </body>
            </html>
          }
        }
      } ~
      /**
       * Returns json array of all universities in a given country.
       */
      path("universities" / IntNumber) { countryCode =>
        respondWithMediaType(`application/json`) {
          complete {
            query {
             (for {
                uni  <- Universities
                city <- Cities
                if city.name    === uni.city
                if city.country === countryCode
              } yield uni.name).list
            }
          }
        }
      } ~
      /**
       * Resonds with password reset form.
       */
      path("change-password" / Rest) { token =>
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Enter your new password:</h1>
                <form name="password-reset-form" action="/change-password" method="POST">
                  <input type="password" size="25" name="password" />
                  <input type="hidden" name="token" value={token} />
                  <input type="submit" value="Submit" />
                </form>
              </body>
            </html>
          }
        }
      } ~
      /**
       * Responds with a page that immidiately redirects to a service that will
       * confirm an email address with the given token.
       */
      path("confirm-email" / Rest) { token=>
        respondWithMediaType(`text/html`) {
          complete {
            lazy val formName = "email-confirmation-form"
            lazy val formSubmit = """
              window.onload = function() {
                document.getElementById('%s').submit()
              }
            """.format(formName)

            <html>
              <body>
                <form name={formName} id={formName} action="/confirm-email" method="POST">
                  <input type="hidden" name="token" value={token} />
                </form>
                <script type="text/javascript">
                  {formSubmit}
                </script>
              </body>
            </html>
          }
        }
      }
    } ~
    post {
      /**
       * Registers the user.
       */
      path("register") {
        formFields('email, 'name, 'phone ?, 'password) { (email, name, phone, password) =>
          respondWithMediaType(`text/plain`) {
            (validateEmail(email) & validatePassword(password)) {
              complete {
                val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
                val id = query(insertProfile(passwordHash, name, phone))
                EmailChangeManager.requestEmailChange(id, email)
                "A confirmation email has been sent to %s!".format(email)
              }
            }
          }
        }
      } ~
      /**
       * Unregister the user.
       */
      path("unregister") {
        (authWithToken | authWithPass) { case (user, session) =>
          formField('name) { name =>
            respondWithMediaType(`application/json`) {
              complete {

                case class Message(
                  message1: String,
                  message2: String)

                implicit val MessageFormat = jsonFormat2(Message)
                implicit val SessMessFormat = jsonFormat2(SessMess[Message])

                SessMess(Some(session), Message(name, "testmessage"))
              }
            }
          }
        }
      } ~
      /**
       * Lets the user change his email address.
       */
      path("change-email") {
        authWithPassNoSession { case user =>
          formField('email) { email =>
            validateEmail(email) {
              complete {
                EmailChangeManager.requestEmailChange(user.id, email)
                SessMess(None, "A confirmation email has been sent to %s!".format(email))
              }
            }
          }
        }
      } ~
      /**
       * Lets the user confirm his email address.
       */
      path("confirm-email") {
        authWithEmailConfirmationToken { token =>
          respondWithMediaType(`text/html`) {
            complete {
              def page(s: String) = {
                <html>
                  <body>
                    <h1>{s}</h1>
                  </body>
                </html>
              }

              EmailChangeManager.confirmEmail(token) match {
                case Some(email) => page("Your email address %s has been confirmed!".format(email))
                case None => page("Your email address was not confirmed...")
              }
            }
          }
        }
      } ~
      /**
       * Allows the user to change password either with normal user/pass
       * authentication or by providing a password reset token obtained from
       * a password reset email.
       */
      path("change-password") {
        (authWithPassNoSession | authWithPasswordResetToken) { case user =>
          formField('password) { password =>
            validatePassword(password) {
              complete {
                query(updatePassword(user, password))
                SessMess(None, "Password successfully changed!")
              }
            }
          }
        }
      } ~
      /**
       * Sends a password reset link to a given email address.
       */
      path("lost-password") {
        formField('email) { email =>
          complete {
            LostPasswordManager.sendResetLink(email)
            SessMess(None, "A reset link has been sent to \'" + email + "\'.")
          }
        }
      } ~
      /**
       * Logs the user out of his current session.
       */
      path("sign-out") {
        authWithTokenNoSession { case user =>
          complete {
            SessMess(None, "You have been signed out!")
          }
        }
      } ~
      /**
       * Deletes all session data that belongs to a user.
       */
      path("delete-session-data") {
        (authWithTokenNoSession | authWithPassNoSession) { case user =>
          complete {
            query(deleteSessionData(user.id))
            SessMess(None, "Your session data has been deleted!")
          }
        }
      }
    } ~
    /**
     * Creates a group
     */
    path("create-group") {
      (authWithToken | authWithPass) { case (user, session) =>
        formFields('name, 'description, 'privacy.as[GroupPrivacy], 'parent.as[Int] ?) { (name, description, privacy, parent) =>
          authorize(user.isMemberOf(parent)) {
            complete {
              val id = query(insertGroup(name, user.id, description, privacy, parent))
              SessMess(Some(session), "Created group %s".format(name))
            }
          }
        }
      }
    }
}

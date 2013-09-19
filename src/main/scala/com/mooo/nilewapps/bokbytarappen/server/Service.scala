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

import scala.concurrent.duration._
import akka.actor.Actor

import spray.util.LoggingContext
import spray.routing._
import spray.routing.authentication._
import spray.http._
import spray.http.StatusCodes._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport

import spray.json.DefaultJsonProtocol
import SprayJsonSupport._
import MediaTypes._

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import TokenJsonProtocol._
import ServiceErrors._

import scala.language.postfixOps

import com.typesafe.config._

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
trait Service extends HttpService with DB with Authenticator {

  def domain = ConfigFactory.load().getString("http-server.domain")

  def authWithPass = authenticate(new BasicHttpAuthenticator("Protected", passwordAuthenticator))

  def authWithPassNoSession =
    authenticate(new BasicHttpAuthenticator("Protected", passwordAuthenticatorNoSession))

  def authWithToken = authenticate(new TokenAuthenticator("Protected", tokenAuthenticator))

  def authWithTokenNoSession =
    authenticate(new TokenAuthenticator("Protected", tokenAuthenticatorNoSession))

  def authWithPasswordResetToken =
    authenticate(new PasswordResetTokenAuthenticator("Password reset", passwordResetTokenAuthenticator))

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
                <h1>New password placeholder.</h1>
                <form name="password-reset-form" action={domain + "/change-password"} method="POST">
                  <input type="password" size="25" name="password" />
                  <input type="hidden" name="token" value={token} />
                  <input type="submit" value="Submit" />
                </form>
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
        formFields('email, 'name, 'phone ?, 'university, 'password) { (email, name, phone, university, password) =>
          respondWithMediaType(`text/plain`) {
            (validate(EmailValidator.isValid(email), InvalidEmail) &
             validate(EmailValidator.isAvailable(email), UnavailableEmail) &
             validate(PasswordValidator.threshold(password), BadPassword)) {
              complete {
                val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
                DBManager.insertProfile(email, passwordHash, name, phone, university)
                "Successfully registered " + email
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
       * Verifies a password and returns an authentication token.
       */
      path("login") {
        authWithPass { case (user, session) =>
          respondWithMediaType(`application/json`) {
            complete {
              SessMess(Some(session), "")
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
            validate(PasswordValidator.threshold(password), BadPassword) {
              complete {
                DBManager.updatePassword(user, password)
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
       * Deletes all session data that belongs to a user.
       */
      path("delete-session-data") {
        (authWithTokenNoSession | authWithPassNoSession) { case user =>
          complete {
            DBManager.deleteSessionData(user.id)
            SessMess(None, "Your session data has been deleted!")
          }
        }
      }
    }
}

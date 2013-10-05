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
import scala.language.postfixOps

import akka.actor.Actor
import com.typesafe.config._
import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data._
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.service._
import com.mooo.nilewapps.bokbytarappen.server.ServiceErrors._
import com.mooo.nilewapps.bokbytarappen.server.util._
import com.mooo.nilewapps.bokbytarappen.server.validation._
import com.mooo.nilewapps.bokbytarappen.server.validation.Validators._

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
trait Service
  extends HttpService
  with PasswordService
  with EmailService
  with GroupService
  with SessionService {

  val routes = {
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
      path("change-password" / Rest) { passwordChangeForm(_) } ~
      path("confirm-email" / Rest) { emailConfirmationPage(_) }
    } ~
    post {
      /**
       * Registers the user.
       */
      path("register") {
        formFields('email, 'name, 'phone ?, 'password) {
          (email, name, phone, password) =>
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
      path("change-email") { changeEmail } ~
      path("confirm-email") { confirmEmail } ~
      path("change-password") { changePassword } ~
      path("lost-password") { lostPassword } ~
      path("sign-out") { signOut } ~
      path("delete-session-data") { deleteSessionData } ~
      path("create-group") { createGroup } ~
      path("join-group") { joinGroup } ~
      path("leave-group") { leaveGroup }
    }
  }
}

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

import scala.concurrent.duration._
import akka.actor.Actor
import spray._
import routing._
import authentication._
import http._
import httpx.SprayJsonSupport
import SprayJsonSupport._
import httpx.marshalling._
import json.DefaultJsonProtocol
import MediaTypes._

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import SessionJsonProtocol._

import scala.language.postfixOps

/**
 * Actor that runs the service
 */
class ServiceActor extends Actor with Service {

  def actorRefFactory = context

  def receive = runRoute(routes ~
    /**
     * Stops the server. Can only be accessed from localhost.
     */
    path("stop") {
      host("localhost") {
        get {
          complete {
            context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }
            "shutdown"
          }
        }
      }
    })

}

/**
 * Provides all functionality of the server
 */
trait Service extends HttpService with DB with Authenticator {

  val routes =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Say hello to <i>Bokbytarappen's server</i>!</h1>
              </body>
            </html>
          }
        }
      }
    } ~
    /**
     * Returns json array of all universities in a given country.
     */
    path("universities" / IntNumber) { countryCode =>
      get {
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
      }
    } ~
    /**
     * Registers the user
     */
    path("register") {
      post {
        formFields('email, 'name, 'phone ?, 'university, 'password) { (email, name, phone, university, password) =>
          respondWithMediaType(`text/plain`) {
            complete {
              query {
                val salt = BCrypt.gensalt()
                val passwordHash = BCrypt.hashpw(password, salt)
                Profiles.insert(Profile(email, passwordHash, salt, name, phone, university))
                "Successfully registered " + email
              }
            }
          }
        }
      }
    } ~
    /**
     * Unregister the user
     */
    path("unregister") {
      post {
        (authenticate(new BasicTokenAuthenticator("Unregistration", tokenAuthenticator))) { case (user, session) =>
          formField('name) { name =>
            respondWithMediaType(`application/json`) {
              complete {

                case class Message(
                  message1: String,
                  message2: String)

                implicit val MessageFormat = jsonFormat2(Message)
                implicit val SessMessFormat = jsonFormat2(SessMess[Message])

                SessMess(session, Message(name, "testmessage"))
              }
            }
          }
        }
      }
    } ~
    /**
     * Verifies a password and returns an authentication token.
     */
    path("login") {
      post {
        authenticate(new BasicHttpAuthenticator("Login", passwordAuthenticator)) { case (user, session) =>
          respondWithMediaType(`application/json`) {
            complete {
              implicit val SessMessFormat = jsonFormat2(SessMess[String])
              SessMess(session, "")
            }
          }

        }
      }
    }
}

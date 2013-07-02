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
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport
import SprayJsonSupport._
import spray.httpx.marshalling._
import spray.json.DefaultJsonProtocol._
import MediaTypes._

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

/**
 *  Actor that runs the service
 */
class ServiceActor extends Actor with Service {

  def actorRefFactory = context

  def receive = runRoute(routes ~
    /**
     * Stopes the server. Can only be accessed from localhost.
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

//case class City(name: String, code: Int)
//object CityJsonProtocol extends DefaultJsonProtocol {
//  implicit val CityFormat = jsonFormat2(City)
//}

/**
 *  Provides all functionality of the server
 */
trait Service extends HttpService with DB {

  //import CityJsonProtocol._ 

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
     *  Returns json array of all universities in a given country.
     */
    path("universities" / IntNumber) { countryCode =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            db withSession {
             (for {
                uni <- Universities
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
     *  Registers the user
     */
    path("register") {
      post {
        formFields('id, 'hash) { (id, hash) =>
          respondWithMediaType(`text/plain`) {
            complete {
              "Your id is '" + id + "' and the hashed id is '" + hash + "'"
            }
          }
        }
      }
    }
    
}

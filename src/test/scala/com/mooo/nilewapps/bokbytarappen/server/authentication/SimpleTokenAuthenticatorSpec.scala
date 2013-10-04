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

import org.specs2.mutable.Specification
import spray.http._
import spray.http.HttpHeaders._
import spray.routing._
import spray.routing.AuthenticationFailedRejection._
import spray.routing.Directives._
import spray.testkit.Specs2RouteTest

class SimpleTokenAuthenticatorSpec
  extends Specification
  with Specs2RouteTest {

  val realm = "Protected"

  def auth = {
    authenticate(new SimpleTokenAuthenticator(realm, t => future { t })) {
      t => complete(t.toString)
    }
  }

  def authFail = {
    authenticate(new SimpleTokenAuthenticator(realm, t => future { None: Option[String] })) {
      t => complete("")
    }
  }

  def challengeHeaders =
    `WWW-Authenticate`(HttpChallenge(
      scheme = "Nilewapp", realm = realm, params = Map.empty)) :: Nil

  def encode(s: String) = {
    val en = new sun.misc.BASE64Encoder()
    new String(en.encode(s.getBytes))
  }

  "SimpleTokenAuthenticator" should {
    "reject if the required request entity field isn't present" in {
      Post() ~> auth ~> check {
        rejection must_==
          AuthenticationFailedRejection(CredentialsMissing, challengeHeaders)
      }
    }
    "reject if the authenticator returns None" in {
      Post("/", FormData(Map("token" -> "value"))) ~> authFail ~> check {
        rejection must_==
          AuthenticationFailedRejection(CredentialsRejected, challengeHeaders)
      }
    }
    "return the result of the authenticator if a correct entity field is present" in {
      Post("/", FormData(Map("token" -> "value"))) ~> auth ~> check {
        responseAs[String] must_== "value"
      }
    }
  }
}

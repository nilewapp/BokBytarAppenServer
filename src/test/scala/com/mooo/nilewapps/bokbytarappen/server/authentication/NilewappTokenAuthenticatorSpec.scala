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

import org.specs2.mutable.Specification

import scala.concurrent._

import spray.testkit.Specs2RouteTest
import spray.http._
import spray.routing._
import spray.routing.Directives._
import HttpHeaders._

import com.mooo.nilewapps.bokbytarappen.server.data.Token

class NilewappTokenAuthenticatorSpec
  extends Specification
  with Specs2RouteTest {

  val realm = "Protected"

  def auth = {
    authenticate(new NilewappTokenAuthenticator(realm, { t => future { t } })) { t =>
      complete(t.toString)
    }
  }

  def encode(s: String) = {
    val en = new sun.misc.BASE64Encoder()
    new String(en.encode(s.getBytes))
  }

  "NilewappTokenAuthenticator" should {
    "reject if no authorization header is present" in {
      Post() ~> auth ~> check {
        rejection === AuthenticationFailedRejection(realm)
      }
    }
    "reject if the authorization scheme is wrong" in {
      Post() ~> addHeader(Authorization( GenericHttpCredentials("Wrong",
          Map("email" -> "", "series" -> "", "token" -> "")))) ~> auth ~> check {
        rejection === AuthenticationFailedRejection(realm)
      }
    }
    "reject if the correct parameters aren't in the header" in {
      Post() ~> addHeader(Authorization(GenericHttpCredentials("Nilewapp",
        Map("email" -> "", "series" -> "")))) ~> auth ~> check {
        rejection === AuthenticationFailedRejection(realm)
      }
      Post() ~> addHeader(Authorization(GenericHttpCredentials("Nilewapp",
        Map("email" -> "", "token" -> "")))) ~> auth ~> check {
        rejection === AuthenticationFailedRejection(realm)
      }
      Post() ~> addHeader(Authorization(GenericHttpCredentials("Nilewapp",
        Map("series" -> "", "token" -> "")))) ~> auth ~> check {
        rejection === AuthenticationFailedRejection(realm)
      }
    }
    "return the result of the authenticator if a correct authorization header is given" in {
      Post() ~> addHeader(Authorization(GenericHttpCredentials("Nilewapp",
        Map("email" -> encode("value1"), "series" -> encode("value2"), "token" -> encode("value3"))))) ~> auth ~> check {
        entityAs[String] must_== Token("value1", "value2", "value3", None).toString
      }
    }
  }
}

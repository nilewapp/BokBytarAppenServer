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

import org.specs2.mutable.Specification
import spray.http._
import spray.http.StatusCodes._
import spray.testkit.Specs2RouteTest

class ServiceSpec
  extends Specification
  with Specs2RouteTest
  with Service {

  def actorRefFactory = system

  "Service" should {
    "return a greeting for GET requests to the root path" in {
      Get() ~> routes ~> check {
        responseAs[String] must contain("Say hello")
      }
    }
  }
}

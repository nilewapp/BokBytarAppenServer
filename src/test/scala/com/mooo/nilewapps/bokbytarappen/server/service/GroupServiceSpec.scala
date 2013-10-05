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

import org.specs2._
import org.specs2.mutable.{BeforeAfter, Specification}
import spray.http._
import spray.http.StatusCodes._
import spray.routing._
import spray.routing.AuthenticationFailedRejection._
import spray.testkit.Specs2RouteTest

class GroupServiceSpec
  extends Specification
  with Specs2RouteTest
  with GroupService {

  def actorRefFactory = system

  "GroupService" should {
    "reject unauthenticahed users" in e1
  }

  def e1 = {
    Post() ~> createGroup ~> check {
      rejections must have size(2)
      rejections map {
        _ match {
          case AuthenticationFailedRejection(CredentialsMissing, _) => ok
          case _ => failure("Expected CredentialsMissing")
        }
      }
    }
  }

  trait ProfileContext extends BeforeAfter {

    var profileId: Int = -1

    def before = {
    }
  }
}

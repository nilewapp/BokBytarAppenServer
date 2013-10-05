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
import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession
import spray.http._
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import spray.routing._
import spray.routing.AuthenticationFailedRejection._
import spray.testkit.Specs2RouteTest

import com.mooo.nilewapps.bokbytarappen.server.data._
import com.mooo.nilewapps.bokbytarappen.server.data.GroupPrivacy._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.ServiceErrors._
import com.mooo.nilewapps.bokbytarappen.server.util._

class GroupServiceSpec
  extends Specification
  with Specs2RouteTest
  with GroupService {

  def actorRefFactory = system

  "createGroup" should {
    "reject requests with no credentials" in e1
    "reject users with invalid credentials" in e2
    "reject requests with missing form fields" in e3
    "reject requests with invalid Privacy values" in e4
    "reject users who try to create child groups of groups they aren't members of" in e5
    "create a global group if the given information is valid" in e6
    "create a child group if the given information is valid" in e7
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

  def e2 = {
    Post() ~>
    addHeader(Authorization(
      BasicHttpCredentials(SecureString(), SecureString()))) ~>
    createGroup ~>
    check {
      rejections must have size(2)
      rejections map {
        _ match {
          case AuthenticationFailedRejection(CredentialsRejected, _) => ok
          case _ => failure("Expected CredentialsRejected")
        }
      }
    }
  }

  def e3 = new ProfileContext {
    Post("/", FormData(Map("name" -> "value1", "description" -> "value2"))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      rejections must have size(2)
      rejections(1) must_== MissingFormFieldRejection("privacy")
    }

    Post("/", FormData(Map("name" -> "value1", "privacy" -> "0"))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      rejections must have size(2)
      rejections(1) must_== MissingFormFieldRejection("description")
    }

    Post("/", FormData(Map("description" -> "value2", "privacy" -> "0"))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      rejections must have size(2)
      rejections(1) must_== MissingFormFieldRejection("name")
    }
  }

  def e4 = new ProfileContext {
    Post("/", FormData(
      Map("name" -> "value1",
        "description" -> "value2",
        "privacy" -> "wibble"))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      rejections must have size(2)
      rejections(1) match {
        case MalformedFormFieldRejection("privacy", _, _) => ok
        case _ => failure("Expected MalformedFormFieldRejection(privacy, _, _)")
      }
    }
  }

  def e5 = new ProfileContext {
    Post("/", FormData(
      Map("name" -> "value1",
        "description" -> "value2",
        "privacy" -> "0",
        "parent" -> Math.abs((new scala.util.Random).nextInt).toString))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      rejections must have size(2)
      rejections(1) match {
        case ValidationRejection(m, _) if String2ServiceError(m) == NotMemberOfParentGroup => ok
        case _ => failure("Expected ValidationRejection(NotMemberOfParentGroup, _)")
      }
    }
  }

  def e6 = new ProfileContext {
    val groupName = SecureString()
    val groupDescription = SecureString()
    Post("/", FormData(
      Map("name" -> groupName,
        "description" -> groupDescription,
        "privacy" -> "0"))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      query {
        val groups = Query(Groups).filter(_.name === groupName).list

        groups must be size(1)

        val h = groups.head

        h.name must_== groupName

        h.description.getSubString(1, h.description.length.toInt) must_==
          groupDescription

        h.privacy must_== Secret

        h.parent must_== None

        Query(Members).filter(q =>
          q.group === h.id && q.profile === profileId).delete must_== 1

        Query(Groups).filter(_.id === h.id).delete must_== 1
      }
    }
  }

  def e7 = new GroupContext {
    val childGroupName = SecureString()
    val childGroupDescription = SecureString()
    Post("/", FormData(
      Map("name" -> childGroupName,
        "description" -> childGroupDescription,
        "privacy" -> "0",
        "parent" -> groupId.toString))) ~>
    addHeader(Authorization(BasicHttpCredentials(user, pass))) ~>
    createGroup ~>
    check {
      query {
        val groups = Query(Groups).filter(_.name === childGroupName).list

        groups must be size(1)

        val h = groups.head

        h.name must_== childGroupName

        h.description.getSubString(1, h.description.length.toInt) must_==
          childGroupDescription

        h.privacy must_== Secret

        h.parent must_== Some(groupId)

        Query(Members).filter(q =>
          q.group === h.id && q.profile === profileId).delete must_== 1

        Query(Groups).filter(_.id === h.id).delete must_== 1
      }
    }
  }

  trait ProfileContext extends BeforeAfter {

    val user = SecureString()
    val pass = SecureString()
    val passHash = BCrypt.hashpw(pass, BCrypt.gensalt())
    var profileId: Int = -1

    def before = query {
      profileId = (Profiles.email ~
       Profiles.passwordHash ~
       Profiles.name ~
       Profiles.phoneNumber) returning Profiles.id insert((
        Some(user), passHash, "", None))
    }

    def after = query {
      Query(Sessions).filter(_.id === profileId).delete
      Query(Profiles).filter(_.email === user).delete
    }
  }

  trait GroupContext extends ProfileContext {

    val groupName = SecureString()
    val groupDescription = SecureString()
    var groupId: Int = -1

    override def before = {
      super.before
      query {
        groupId = insertGroup(groupName, profileId, groupDescription, Public, None)
        Members.insert((groupId, profileId))
      }
    }

    override def after = {
      query {
        Query(Members).filter(q =>
          q.group === groupId && q.profile === profileId).delete
        Query(Groups).filter(_.id === groupId).delete
      }
      super.after
    }
  }
}

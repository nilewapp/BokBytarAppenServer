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

import scala.slick.driver.H2Driver.simple._
import scala.slick.driver.H2Driver.simple.Database.threadLocalSession

import com.typesafe.config._
import org.h2.jdbc.JdbcSQLException
import org.specs2.mutable.{BeforeAfter, Specification}

import com.mooo.nilewapps.bokbytarappen.server.data._
import com.mooo.nilewapps.bokbytarappen.server.DB._

class DBSpec extends Specification {

  "This test" should {
    "load configuration file from the test directory" in {
      ConfigFactory.load().getString("db.name") must_== "test"
    }
  }
  "DB" should {
    "be able to insert groups with no parents" in new GroupContext {
      query {
        val groups = Query(Groups).filter(_.id === groupId).list

        groups must be size(1)

        val h = groups.head

        h.id must_== groupId

        h.name must_== "Name"

        h.owner must_== profileId

        h.description.getSubString(1, h.description.length.toInt) must_==
          "Description"

        h.privacy must_== Public

        h.parent must_== None
      }
    }
    "reject insert of group with non existing parent" in new ProfileContext {
      val parent = Math.abs((new scala.util.Random).nextInt)
      query(insertGroup("Name", profileId, "Description", Public, Some(parent))) must
        throwA[JdbcSQLException]
    }
    "be able to insert groups with a parent" in new ChildGroupContext {
      query {
        val groups = Query(Groups).filter(_.id === childId).list

        groups must be size(1)

        val h = groups.head

        h.id must_== childId

        h.name must_== "Child"

        h.owner must_== profileId

        h.description.getSubString(1, h.description.length.toInt) must_==
          "Child description"

        h.privacy must_== Public

        h.parent must_== Some(groupId)
      }
    }
  }

  trait ProfileContext extends BeforeAfter {

    var profileId: Int = -1

    def before = query {
      profileId = insertProfile("", "", None)
    }

    def after = query {
      Query(Profiles).filter(_.id === profileId).delete
    }
  }

  trait GroupContext extends ProfileContext {

    var groupId: Int = -1

    override def before = query {
      super.before
      groupId = insertGroup("Name", profileId, "Description", Public, None)
    }

    override def after = query {
      Query(Groups).filter(_.id === groupId).delete
      super.after
    }
  }

  trait ChildGroupContext extends GroupContext {

    var childId: Int = -1

    override def before = {
      super.before
      childId = query {
        insertGroup(
          "Child",
          profileId,
          "Child description",
          Public,
          Some(groupId))
      }
    }

    override def after = {
      query(Query(Groups).filter(_.id === childId).delete)
      super.after
    }
  }
}

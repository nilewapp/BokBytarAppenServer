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

import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.data._

import com.typesafe.config._

import org.specs2.mutable.{BeforeAfter, Specification}

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

class DBSpec extends Specification {

  "This test" should {
    "load configuration file from the test directory" in {
      ConfigFactory.load().getString("db.name") must_== "test"
    }
  }
  "DB" should {
    "be able to insert groups with no parents" in new Context {
      query {
        val groups = Query(Groups).filter(_.id === groupId).list
        groups must be size(1)
        val h = groups.head
        h.id must_== groupId
        h.name must_== "Name"
        h.owner must_== profileId
        h.description.getSubString(1, h.description.length.toInt) must_== "Description"
        h.privacy must_== Public
        h.parent must_== None
      }
    }
    "be able to insert groups with a parent" in new ChildContext {
      query {
        val groups = Query(Groups).filter(_.id === childId).list
        groups must be size(1)
        val h = groups.head
        h.id must_== childId
        h.name must_== "Child"
        h.owner must_== profileId
        h.description.getSubString(1, h.description.length.toInt) must_== "Child description"
        h.privacy must_== Public
        h.parent must_== Some(groupId)
      }
    }
  }

  trait Context extends BeforeAfter {

    var profileId: Int = -1
    var groupId: Int = -1

    def before = query {
      profileId = insertProfile("", "", None)
      groupId = insertGroup("Name", profileId, "Description", Public, None)
    }

    def after = query {
      Query(Groups).filter(_.id === groupId).delete
      Query(Profiles).filter(_.id === profileId).delete
    }
  }

  trait ChildContext extends Context {

    var childId: Int = -1

    override def before = {
      super.before
      childId = query(insertGroup("Child", profileId, "Child description", Public, Some(groupId)))
    }

    override def after = {
      query(Query(Groups).filter(_.id === childId).delete)
      super.after
    }
  }
}

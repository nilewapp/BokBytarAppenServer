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

import scala.language.postfixOps

import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data.GroupPrivacy._
import com.mooo.nilewapps.bokbytarappen.server.data.SessMessJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.data.{GroupPrivacy, SessMess}
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.ServiceErrors._

/**
 * Contains routes for user management of Groups
 */
trait GroupService extends HttpService {

  /**
   * Creates a group.
   */
  def createGroup = {
    (authWithToken | authWithPass) { case (user, session) =>
      formFields(
        'name,
        'description,
        'privacy.as[GroupPrivacy],
        'parent.as[Int] ?) { (name, description, privacy, parent) =>
        authorize(user.isMemberOf(parent)) {
          complete {
            query(insertGroup(name, user.id, description, privacy, parent))
            SessMess(Some(session), "Created group %s".format(name))
          }
        }
      }
    }
  }

  /**
   * Adds a user to a group.
   */
  def joinGroup = {
    (authWithToken | authWithPass) { case (user, session) =>
      formField('group.as[Int]) { groupId =>
        val group = query {
          Query(Groups).filter(_.id === groupId).take(1).list.headOption
        }
        (validate(group != None, NonExistingGroup) &
         validate(!user.isMemberOf(Some(groupId)), AlreadyMemberOfGroup) &
         validate(user.isMemberOf(group.get.parent), NotMemberOfParentGroup)) {
          complete {
            query(Members.insert((groupId, user.id)))
            SessMess(Some(session), "Joined group %s".format(group.get.name))
          }
        }
      }
    }
  }

  /**
   * Removes a user from a group.
   */
  def leaveGroup = {
    (authWithToken | authWithPass) { case (user, session) =>
      formField('group.as[Int]) { groupId =>
        val group = query {
          Query(Groups).filter(_.id === groupId).take(1).list.headOption
        }
        (validate(group != None, NonExistingGroup) &
         validate(user.isMemberOf(Some(groupId)), NotMemberOfGroup) &
         validate(user.isMemberOfChild(Some(groupId)), MemberOfChildGroup)) {
          complete {
            query {
              Query(Members).filter(q =>
                q.group === groupId && q.profile === user.id).delete
            }
            SessMess(Some(session), "Left group %s".format(group.get.name))
          }
        }
      }
    }
  }
}

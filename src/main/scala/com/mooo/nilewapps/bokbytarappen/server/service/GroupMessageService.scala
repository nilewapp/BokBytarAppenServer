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

import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession
import spray.httpx.SprayJsonSupport._
import spray.routing.Directives._

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data.SessMess
import com.mooo.nilewapps.bokbytarappen.server.data.SessMessJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.ServiceErrors._

/**
 * Contains routes for posting and responding to messages in Groups.
 */
trait GroupMessageService {

  /**
   * Post a new Message in a Group.
   */
  def postGroupMessage = {
    (authWithToken | authWithPass) { case (user, sess) =>
      formFields('recipient.as[Int], 'title, 'content) { (r, t, c) =>
        (validate(!t.isEmpty, TitleTooShort) &
         validate(!c.isEmpty, ContentTooShort) &
         validate(user.isMemberOf(Some(r)), NotMemberOfGroup)) {
          complete {
            query {
              insertGroupMessage(user.id, r, Some(t), c, None)
            }
            SessMess(Some(sess), "Posted message")
          }
        }
      }
    }
  }

  /**
   * Respond to a Message in Group.
   */
  def respondToGroupMessage = {
    (authWithToken | authWithPass) { case (user, sess) =>
      formFields('parent.as[Int], 'content) { (parent, content) =>
        validate(!content.isEmpty, ContentTooShort) {
          val parentMessage =
            Query(GroupMessages).filter(_.id === parent).list.headOption
          validate(parentMessage != None, ResponseToNothing) {
            complete {
              query {
                insertGroupMessage(
                  user.id,
                  parentMessage.get.recipient,
                  None,
                  content,
                  Some(parent))
              }
              SessMess(Some(sess), "Responded to message")
            }
          }
        }
      }
    }
  }
}

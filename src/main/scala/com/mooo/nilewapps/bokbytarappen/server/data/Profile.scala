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
package com.mooo.nilewapps.bokbytarappen.server.data

import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.DB._
import com.mooo.nilewapps.bokbytarappen.server.util.BCrypt

case class Profile(
  id: Int,
  email: Option[String],
  passwordHash: String,
  name: String,
  phoneNumber: Option[String]) {

  /**
   * Delete all session data of the user.
   */
  def deleteSessionData() {
    Query(Sessions).filter(_.id === id).delete
  }

  /**
   * Deletes all session data and updates the email address of the user.
   */
  def updateEmail(newEmail: Option[String]): Option[Profile] = {
    deleteSessionData()
    Query(EmailConfirmationTokens).filter(_.id === id).delete
    val p = Profile(id, newEmail, passwordHash, name, phoneNumber)
    Query(Profiles).filter(_.id === id).update(p) match {
      case 1 => Some(p)
      case _ => None
    }
  }

  /**
   * Delete all session data of a specific user and update its password.
   */
  def updatePassword(password: String): Option[Profile] = {
    deleteSessionData()
    Query(PasswordResetTokens).filter(_.id === id).delete

    val p = Profile(
      id, email, BCrypt.hashpw(password, BCrypt.gensalt()), name, phoneNumber)

    Query(Profiles).filter(_.id === id).update(p) match {
      case 1 => Some(p)
      case _ => None
    }
  }

  /**
   * Returns true if the user is a member of the given group.
   */
  def isMemberOf(groupId: Option[Int]): Boolean = query {
    groupId match {
      case Some(i) =>
        Query(Members).filter(q =>
          q.group === i && q.profile === id).list.length == 1
      case None => true
    }
  }

  /**
   * Returns true if the member of any child group of a given group.
   */
  def isMemberOfChild(parentId: Option[Int]): Boolean = query {
    parentId match {
      case Some(i) =>
        (for {
          g <- Groups
          m <- Members
          if m.profile === id &&
             m.group === g.id &&
             g.parent === parentId
        } yield g.name).list.isEmpty
      case None =>
        (for {
          m <- Members
          if m.profile === id
        } yield m).list.isEmpty
    }
  }
}

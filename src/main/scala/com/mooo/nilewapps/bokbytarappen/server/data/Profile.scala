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

import com.mooo.nilewapps.bokbytarappen.server.DB._

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

case class Profile(
  id: Int,
  email: Option[String],
  passwordHash: String,
  name: String,
  phoneNumber: Option[String]) {

  def isMemberOf(groupId: Option[Int]): Boolean = query {
    groupId match {
      case Some(i) =>
        Query(Members).filter(q => q.group === i && q.profile === id).list.length == 1
      case None => true
    }
  }
}

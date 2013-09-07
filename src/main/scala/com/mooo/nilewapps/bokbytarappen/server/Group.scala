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

import slick.lifted.MappedTypeMapper

case class Group(
  id: Option[Int],
  name: String,
  owner: Int,
  description: String,
  privacy: GroupPrivacy,
  parent: Option[Int])

sealed trait GroupPrivacy
case object Secret extends GroupPrivacy
case object Closed extends GroupPrivacy
case object Public extends GroupPrivacy

object GroupPrivacy {

  implicit val groupPrivacyTypeMapper = MappedTypeMapper.base[GroupPrivacy, Int]({ 
      _ match {
        case Secret => 0
        case Closed => 1
        case Public => 2
      }
    }, {
      _ match {
        case 0 => Secret
        case 1 => Closed
        case 2 => Public
      }
    })
}

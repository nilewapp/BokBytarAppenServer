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

import java.sql.Clob

import slick.lifted.MappedTypeMapper

import spray.httpx.unmarshalling.{DeserializationError, Deserializer, MalformedContent}

case class Group(
  id: Int,
  name: String,
  owner: Int,
  description: Clob,
  privacy: GroupPrivacy,
  parent: Option[Int])

sealed trait GroupPrivacy
case object Secret extends GroupPrivacy
case object Closed extends GroupPrivacy
case object Public extends GroupPrivacy

object GroupPrivacy {

  implicit val String2GroupPrivacyConverter = new Deserializer[String, GroupPrivacy] {
    def apply(value: String) = {
      try {
        value.toInt match {
          case 0 => Right(Secret)
          case 1 => Right(Closed)
          case 2 => Right(Public)
          case _ => groupPrivacyFormatError(value)
        }
      } catch {
        case e: NumberFormatException =>
          groupPrivacyFormatError(value)
      }
    }

    private[this] def groupPrivacyFormatError(value: String): Either[DeserializationError, Nothing] =
      Left(MalformedContent("'%s' is not a valid group privacy value".format(value)))
  }

  implicit val GroupPrivacyTypeMapper = MappedTypeMapper.base[GroupPrivacy, Int]({
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

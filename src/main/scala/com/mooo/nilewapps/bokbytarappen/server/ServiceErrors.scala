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

import scala.language.implicitConversions

import spray.json._

import com.mooo.nilewapps.bokbytarappen.server.ServiceErrorCodes._

sealed case class ServiceError(code: Int, reason: String)

object ServiceErrorJsonProtocol extends DefaultJsonProtocol {
  implicit val serviceErrorFormat = jsonFormat2(ServiceError)
}

/**
 * Defines service error codes.
 */
object ServiceErrors {

  import com.mooo.nilewapps.bokbytarappen.server.ServiceErrorJsonProtocol._

  implicit def String2ServiceError(s: String) =
    s.asJson.convertTo[ServiceError]

  implicit def ServiceError2String(e: ServiceError) =
    e.toJson.toString

  val BadPassword =
    ServiceError(
      BAD_PASSWORD,
      "Password doesn't have sufficient guessing entropy.")

  val UnavailableEmail =
    ServiceError(
      UNAVAILABLE_EMAIL,
      "Email address is already registered with another account.")

  val InvalidEmail =
    ServiceError(
      INVALID_EMAIL,
      "Email address is not valid.")

  val NonExistingGroup =
    ServiceError(
      NON_EXISTING_GROUP,
      "Group does not exist.")

  val NotMemberOfParentGroup =
    ServiceError(
      NOT_MEMBER_OF_PARENT_GROUP,
      "You are not a member of the parent group.")

  val AlreadyMemberOfGroup =
    ServiceError(
      ALREADY_MEMBER_OF_GROUP,
      "You are already a member of the group.")

  val NotMemberOfGroup =
    ServiceError(
      NOT_MEMBER_OF_GROUP,
      "You are not a member of the group.")

  val MemberOfChildGroup =
    ServiceError(
      MEMBER_OF_CHILD_GROUP,
      "You are still a member of a child group.")

  val TitleTooShort =
    ServiceError(
      TITLE_TOO_SHORT,
      "The title of your post is too short.")

  val ContentTooShort =
    ServiceError(
      CONTENT_TOO_SHORT,
      "The content of your post is too short.")

  val ResponseToNothing =
    ServiceError(
      RESPONSE_TO_NOTHING,
      "You tried to respond to a message that doesn't exist.")
}

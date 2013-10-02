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

import ServiceErrorCodes._

sealed case class ServiceError(val code: Int, val reason: String)

/**
 * Defines service error codes.
 */
object ServiceErrors {

  implicit def ServiceError2String(e: ServiceError) =
    JsObject(
      ("code", JsNumber(e.code)),
      ("reason", JsString(e.reason))).toString

  val BadPassword =
    ServiceError(
      BAD_PASSWORD,
      "Password doesn't have sufficient guessing entropy.")

  val UnavailableEmail =
    ServiceError(
      UNAVAILABLE_EMAIL,
      "Email address is already registered with another account.")

  val InvalidEmail =
    ServiceError(INVALID_EMAIL,
      "Email address is not valid.")
}

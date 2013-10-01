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
package com.mooo.nilewapps.bokbytarappen.server.validation

import slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.DB._

/**
 * Defines methods to validate email addresses.
 */
object EmailValidator {

  val emailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$""".r

  /**
   * Returns true if the given email address matches the format of a
   * valid email address.
   */
  def isValid(email: String) = emailRegex.findFirstIn(email).isDefined

  /**
   * Returns true if the given email address is not already registered.
   */
  def isAvailable(email: String) = query(!getProfile(email).isDefined)
}

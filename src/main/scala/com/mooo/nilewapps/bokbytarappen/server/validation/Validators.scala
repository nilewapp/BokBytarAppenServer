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

import spray.routing.Directives.validate

import com.mooo.nilewapps.bokbytarappen.server.ServiceErrors._

trait Validators {

  /**
   * Asserts that an email address is valid and available.
   */
  def validateEmail(email: String) =
    validate(EmailValidator.isValid(email), InvalidEmail) &
    validate(EmailValidator.isAvailable(email), UnavailableEmail)

  /**
   * Asserts that a password has sufficient guessing entropy.
   */
  def validatePassword(password: String) =
    validate(PasswordValidator.threshold(password), BadPassword)
}

object Validators extends Validators

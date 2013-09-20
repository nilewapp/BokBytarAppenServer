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

import java.security.SecureRandom
import java.math.BigInteger
import sun.misc.BASE64Encoder

object SecureString {

  /**
   * Generates a cryptographically secure random string.
   */
  def apply() = {
    lazy val sr = new SecureRandom
    lazy val token = new BigInteger(130, sr).toString(64).getBytes
    new String(new BASE64Encoder().encode(token))
  }
}

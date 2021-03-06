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
package com.mooo.nilewapps.bokbytarappen.server.util

import java.security.MessageDigest

import sun.misc.BASE64Encoder

object SHA256 {

  def md = MessageDigest.getInstance("SHA-256")

  /**
   * Performs a SHA-256 hash on a string.
   */
  def apply(s: String) = new String(md.digest(s.getBytes))
}

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

/**
 * Produces a Token from a Map.
 */
object TokenFactory {

  /**
   * Produces an Option[Token] from a Map containing the keys "email", "series"
   * and "token". If any of these keys don't exist None is returned.
   */
  def apply(vals: Map[String, String]): Option[Token] = 
    try {
      Some(Token(vals("email"), vals("series"), vals("token"), None))
    } catch {
      case e: NoSuchElementException =>  None
    }
}

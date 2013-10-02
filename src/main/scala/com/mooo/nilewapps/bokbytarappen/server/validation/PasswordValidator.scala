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

/**
 * Computes the strength of a password.
 */
object PasswordValidator {

  /**
   * Returns the given password if its guessing entropy is above a
   * certain threshold.
   */
  def threshold(pass: String, thresh: Int = 18) = strength(pass) > thresh

  /**
   * Estimates the guessing entropy of a password by calucalting its
   * Shannon entropy and applying an arbitrary scaling factor.
   *
   * TODO: Improve estimation, possibly like described
   * <a href="http://cubicspot.blogspot.co.uk/2011/11/how-to-calculate-password-strength.html">here</a>.
   */
  def strength(pass: String) = {

    val entropyScaling = 0.4

    def chars(s: String, i: Int) =
      s.r.findFirstIn(pass) match {
        case Some(_) => i
        case None => 0
      }

    def log2(a: Double) = Math.log(a) / Math.log(2)

    log2(chars("""[a-z]""", 26) +
         chars("""[A-Z]""", 26) +
         chars("""[0-9]""", 10) +
         chars("""[ `~!@#\$%\^&\*\(\)_\+\-{}\|\[\]\\:";'<>\?,\./"]""", 34)) *
       pass.length * entropyScaling
  }

}

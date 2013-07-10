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

import java.nio.ByteBuffer

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Provides time synchronised one time password functionality
 */
object TimeOneTimePassword {

  /**
   * Takes a Base64 encoded secret key and returns a password as a six 
   * digit String based on the current unix time and the secret key
   */
  def apply(secret: String) = {
    val mac = Mac.getInstance("HmacSHA1")
    val key = new sun.misc.BASE64Decoder().decodeBuffer(secret)

    val keySpec = new SecretKeySpec(key, mac.getAlgorithm)
    mac.init(keySpec)

    val message = System.currentTimeMillis / 30000L

    val hash = mac.doFinal(message.toString.getBytes)

    /* Get last nibble of hash */
    val offset = 0xF & hash.last

    /* Take four bytes of the hash */
    val truncatedHash = hash.slice(offset, offset + 4)

    /* Clear the top bit */
    truncatedHash(0) = (0x7F & truncatedHash(0)).toByte

    val wrapper = ByteBuffer.wrap(truncatedHash)
    val code = wrapper.getInt % 1000000

    "%06d".format(code)
  }
}

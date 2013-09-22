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
package com.mooo.nilewapps.bokbytarappen.server.authentication

import scala.concurrent._
import ExecutionContext.Implicits.global

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server
import server.data._
import server.util._
import server.DB._

trait EmailConfirmationTokenAuthenticator {

  /**
   * Takes a email confirmation token string and returns the corresponding token in the database.
   */
  def emailConfirmationTokenAuthenticator(token: Option[String]): Future[Option[EmailConfirmationToken]] = future {
    token match {
      case Some(t) => query {
        Query(EmailConfirmationTokens).filter(q =>
          q.token === SHA256(t) &&
          q.expirationTime > System.currentTimeMillis()).take(1).list.headOption
      }
      case _ => None
    }
  }
}

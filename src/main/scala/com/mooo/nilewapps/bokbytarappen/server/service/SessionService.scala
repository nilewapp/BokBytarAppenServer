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
package com.mooo.nilewapps.bokbytarappen.server.service

import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession
import spray.httpx.SprayJsonSupport._
import spray.routing.Directives._

import com.mooo.nilewapps.bokbytarappen.server.authentication.Authenticators._
import com.mooo.nilewapps.bokbytarappen.server.data.SessMess
import com.mooo.nilewapps.bokbytarappen.server.data.SessMessJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.data.TokenJsonProtocol._
import com.mooo.nilewapps.bokbytarappen.server.DB

/**
 * Contains routes for user session management.
 */
trait SessionService {

  /**
   * Logs the user out of his current session.
   */
  def signOut = {
    authWithTokenNoSession { case user =>
      complete {
        SessMess(None, "You have been signed out!")
      }
    }
  }

  /**
   * Deletes all session data that belongs to a user.
   */
  def deleteSessionData = {
    (authWithTokenNoSession | authWithPassNoSession) { case user =>
      complete {
        DB.query(user.deleteSessionData())
        SessMess(None, "Your session data has been deleted!")
      }
    }
  }
}

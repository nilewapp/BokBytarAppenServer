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

import org.specs2.time.NoTimeConversions
import org.specs2.mutable.{NameSpace, Specification, BeforeAfter}

import scala.concurrent._
import duration._
import ExecutionContext.Implicits.global

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server
import server.DB._
import server.data._
import server.util._

class PasswordAuthenticatorSpec
  extends Specification 
  with PasswordAuthenicator 
  with NoTimeConversions {
}
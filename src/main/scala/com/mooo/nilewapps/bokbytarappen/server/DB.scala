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

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

import com.typesafe.config._

case class Profile(
  id: Int,
  email: Option[String],
  passwordHash: String,
  name: String,
  phoneNumber: Option[String],
  university: String)

case class Session(
  id: Int,
  series: String,
  tokenHash: String,
  expirationTime: Long)

case class SimpleToken(
  id: Int,
  token: String,
  expirationTime: Long)

case class EmailConfirmationToken(
  id: Int,
  token: String,
  email: String)

/**
 * Defines tables and provides database access
 */
object DB {

  def config = ConfigFactory.load().getConfig("db")

  def dbPath = getClass.getResource("/") + config.getString("name")

  def url = "jdbc:h2:%s;USER=%s;PASSWORD=%s".format(dbPath, config.getString("user"), config.getString("pass"))

  implicit val db = Database.forURL(url, driver = "org.h2.Driver")

  /**
   * Tables
   */
  object Countries extends Table[(Int, String)]("COUNTRIES") {
    def numericCode = column[Int]("NUMERIC_CODE", O.PrimaryKey)
    def alpha2Code = column[String]("ALPHA2_CODE")
    def * = numericCode ~ alpha2Code
  }

  object Cities extends Table[(String, Int)]("CITIES") {
    def name = column[String]("NAME", O.PrimaryKey)
    def country = column[Int]("COUNTRY")
    def * = name ~ country
    def countryFK = foreignKey("COUTRY_FK", country, Countries)(_.numericCode)
  }

  object Universities extends Table[(String, String)]("UNIVERSITIES") {
    def name = column[String]("NAME", O.PrimaryKey)
    def city = column[String]("CITY")
    def * = name ~ city
    def cityFK = foreignKey("CITY_FK", city, Cities)(_.name)
  }

  object Profiles extends Table[Profile]("PROFILES") {
    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def email = column[Option[String]]("EMAIL")
    def passwordHash = column[String]("PASSWORD_HASH")
    def name = column[String]("NAME")
    def phoneNumber = column[Option[String]]("PHONE_NUMBER")
    def university = column[String]("UNIVERSITY")
    def * = id ~ email ~ passwordHash ~ name ~ phoneNumber ~ university <> (Profile, Profile.unapply _)
    def emailIndex = index("PROFILES_EMAIL_INDEX", email, unique = true)
    def universityFK = foreignKey("UNIVERSITY_FK", university, Universities)(_.name)
  }

  object WantedBooks extends Table[(Int, Int)]("WANTED_BOOKS") {
    def profileId = column[Int]("PROFILE_ID")
    def isbn13 = column[Int]("ISBN")
    def * = profileId ~ isbn13
    def wantedBooksPK = primaryKey("WANTED_BOOKS_PK", *)
    def profileFK = foreignKey("WANTED_PROFILE_FK", profileId, Profiles)(_.id)
  }

  object OwnedBooks extends Table[(Int, Int)]("OWNED_BOOKS") {
    def profileId = column[Int]("PROFILE_ID")
    def isbn13 = column[Int]("ISBN")
    def * = profileId ~ isbn13
    def ownedBooksPK = primaryKey("OWNED_BOOKS_PK", *)
    def profileFK = foreignKey("OWNED_PROFILE_FK", profileId, Profiles)(_.id)
  }

  object Sessions extends Table[Session]("SESSION") {
    def id = column[Int]("PROFILE")
    def series = column[String]("SERIES")
    def tokenHash = column[String]("TOKEN")
    def expirationTime = column[Long]("EXPIRATION_TIME")
    def * = id ~ series ~ tokenHash ~ expirationTime <> (Session, Session.unapply _)
    def sessionPK = primaryKey("SESSION_PK", id ~ series)
    def profileFK = foreignKey("SESSION_PROFILE_FK", id, Profiles)(_.id)
  }

  import GroupPrivacy._
  object Groups extends Table[Group]("GROUPS") {
    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def name = column[String]("NAME")
    def owner = column[Int]("OWNER")
    def description = column[String]("DESCRIPTION")
    def privacy = column[GroupPrivacy]("PRIVACY")
    def parent = column[Option[Int]]("PARENT")
    def * = id.? ~ name ~ owner ~ description ~ privacy ~ parent <> (Group, Group.unapply _)
    def ownerFK = foreignKey("GROUPS_OWNER_FK", owner, Profiles)(_.id)
  }

  object Members extends Table[(Int, Int)]("MEMBERS") {
    def group = column[Int]("GROUP")
    def profile = column[Int]("PROFILE")
    def * = group ~ profile
    def membersPK = primaryKey("MEMBERS_PK", *)
    def groupFK = foreignKey("MEMBERS_GROUP_FK", group, Groups)(_.id)
    def profileFK = foreignKey("MEMBERS_PROFILE_FK", profile, Profiles)(_.id)
  }

  object PasswordResetTokens extends Table[SimpleToken]("PASSWORD_RESET_TOKENS") {
    def id = column[Int]("ID")
    def token = column[String]("TOKEN")
    def expirationTime = column[Long]("EXPIRATION_TIME")
    def * = id ~ token ~ expirationTime <> (SimpleToken, SimpleToken.unapply _)
    def passwordResetTokensPK = primaryKey("PASSWORD_RESET_TOKENS_PK", id ~ token)
    def profileFK = foreignKey("PASSWORD_RESET_TOKENS_PROFILE_FK", id, Profiles)(_.id)
  }

  object EmailConfirmationTokens extends Table[EmailConfirmationToken]("EMAIL_CONFIRMATION_TOKENS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def token = column[String]("TOKEN")
    def email = column[String]("EMAIL")
    def * = id ~ token ~ email <> (EmailConfirmationToken, EmailConfirmationToken.unapply _)
    def profileFK = foreignKey("EMAIL_CONFIRMATION_TOKENS_PROFILE_FK", id, Profiles)(_.id)
  }

  def query[T](f: => T): T = db withSession f

  /**
   * Inserts a new profile into the database.
   */
  def insertProfile(passwordHash: String, name: String, phoneNumber: Option[String], university: String) = {
    (Profiles.passwordHash ~
     Profiles.name ~
     Profiles.phoneNumber ~
     Profiles.university) returning Profiles.id insert(
       (passwordHash, name, phoneNumber, university))
  }

  /**
   * Delete all session data for a specific user.
   */
  def deleteSessionData(id: Int) = {
    Query(Sessions).filter(_.id === id).delete
    Query(PasswordResetTokens).filter(_.id === id).delete
  }

  def updateEmail(user: Profile, email: Option[String]) = {
    deleteSessionData(user.id)
    Query(Profiles).filter(_.id === user.id).update(
      Profile(
        user.id,
        email,
        user.passwordHash,
        user.name,
        user.phoneNumber,
        user.university))
  }

  /**
   * Delete all session data of a specific user and update its password.
   */
  def updatePassword(user: Profile, password: String) = {
    deleteSessionData(user.id)
    Query(Profiles).filter(_.id === user.id).update(
      Profile(
        user.id,
        user.email,
        BCrypt.hashpw(password, BCrypt.gensalt()),
        user.name,
        user.phoneNumber,
        user.university))
  }

  /**
   * Queries a Profile by id.
   */
  def getProfile(id: Int) = Query(Profiles).filter(_.id === id).take(1).list.headOption

  /**
   * Queries a Profile by email.
   */
  def getProfile(email: String) = Query(Profiles).filter(_.email === email).take(1).list.headOption
}



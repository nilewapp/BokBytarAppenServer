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

import java.sql.Clob

import com.typesafe.config._
import slick.driver.H2Driver.simple._
import slick.driver.H2Driver.simple.Database.threadLocalSession

import com.mooo.nilewapps.bokbytarappen.server.data.GroupPrivacy._
import com.mooo.nilewapps.bokbytarappen.server.util._

/**
 * Defines tables and provides database access
 */
object DB {

  def config = ConfigFactory.load().getConfig("db")

  def dbPath = getClass.getResource("/") + config.getString("name")

  def url = "jdbc:h2:%s;USER=%s;PASSWORD=%s".format(
    dbPath, config.getString("user"), config.getString("pass"))

  def db = Database.forURL(url, driver = "org.h2.Driver")

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

  object Profiles extends Table[data.Profile]("PROFILES") {

    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def email = column[Option[String]]("EMAIL")
    def passwordHash = column[String]("PASSWORD_HASH")
    def name = column[String]("NAME")
    def phoneNumber = column[Option[String]]("PHONE_NUMBER")

    def * = id ~ email ~ passwordHash ~ name ~ phoneNumber <>
      (data.Profile, data.Profile.unapply _)

    def emailIndex = index("PROFILES_EMAIL_INDEX", email, unique = true)
  }

  object WantedBooks extends Table[(Int, Int)]("WANTED_BOOKS") {

    def profileId = column[Int]("PROFILE_ID")
    def isbn13 = column[Int]("ISBN")

    def * = profileId ~ isbn13

    def wantedBooksPK = primaryKey("WANTED_BOOKS_PK", *)

    def profileFK =
      foreignKey("WANTED_PROFILE_FK", profileId, Profiles)(_.id)
  }

  object OwnedBooks extends Table[(Int, Int)]("OWNED_BOOKS") {

    def profileId = column[Int]("PROFILE_ID")
    def isbn13 = column[Int]("ISBN")

    def * = profileId ~ isbn13

    def ownedBooksPK = primaryKey("OWNED_BOOKS_PK", *)

    def profileFK = foreignKey("OWNED_PROFILE_FK", profileId, Profiles)(_.id)
  }

  object Sessions extends Table[data.Session]("SESSION") {

    def id = column[Int]("PROFILE")
    def seriesHash = column[String]("SERIES")
    def tokenHash = column[String]("TOKEN")
    def expirationTime = column[Long]("EXPIRATION_TIME")

    def * = id ~ seriesHash ~ tokenHash ~ expirationTime <>
      (data.Session, data.Session.unapply _)

    def sessionPK = primaryKey("SESSION_PK", id ~ seriesHash)

    def profileFK = foreignKey("SESSION_PROFILE_FK", id, Profiles)(_.id)
  }

  object Groups extends Table[data.Group]("GROUPS") {

    def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def name = column[String]("NAME")
    def owner = column[Int]("OWNER")
    def description = column[Clob]("DESCRIPTION")
    def privacy = column[data.GroupPrivacy]("PRIVACY")
    def parent = column[Option[Int]]("PARENT")

    def * = id ~ name ~ owner ~ description ~ privacy ~ parent <>
      (data.Group, data.Group.unapply _)

    def forInsert = name ~ owner ~ description ~ privacy ~ parent

    def ownerFK = foreignKey("GROUPS_OWNER_FK", owner, Profiles)(_.id)

    def parentFK = foreignKey("GROUPS_PARENT_FK", parent, Groups)(_.id)
  }

  object Members extends Table[(Int, Int)]("MEMBERS") {

    def group = column[Int]("GROUP")
    def profile = column[Int]("PROFILE")

    def * = group ~ profile

    def membersPK = primaryKey("MEMBERS_PK", *)

    def groupFK = foreignKey("MEMBERS_GROUP_FK", group, Groups)(_.id)

    def profileFK = foreignKey("MEMBERS_PROFILE_FK", profile, Profiles)(_.id)
  }

  object PasswordResetTokens
    extends Table[data.SimpleToken]("PASSWORD_RESET_TOKENS") {

    def id = column[Int]("ID", O.PrimaryKey)
    def token = column[String]("TOKEN")
    def expirationTime = column[Long]("EXPIRATION_TIME")

    def * = id ~ token ~ expirationTime <>
      (data.SimpleToken, data.SimpleToken.unapply _)

    def tokenIndex =
      index("PASSWORD_RESET_TOKENS_TOKEN_INDEX", token, unique = true)

    def profileFK =
      foreignKey("PASSWORD_RESET_TOKENS_PROFILE_FK", id, Profiles)(_.id)
  }

  object EmailConfirmationTokens
    extends Table[data.EmailConfirmationToken]("EMAIL_CONFIRMATION_TOKENS") {

    def id = column[Int]("ID", O.PrimaryKey)
    def token = column[String]("TOKEN")
    def email = column[String]("EMAIL")
    def expirationTime = column[Long]("EXPIRATION_TIME")

    def * = id ~ token ~ email ~ expirationTime <>
      (data.EmailConfirmationToken, data.EmailConfirmationToken.unapply _)

    def tokenIndex =
      index("EMAIL_CONFIRMATION_TOKENS_TOKEN_INDEX", token, unique = true)

    def profileFK =
      foreignKey("EMAIL_CONFIRMATION_TOKENS_PROFILE_FK", id, Profiles)(_.id)
  }

  object Followers extends Table[(Int, Int)]("FOLLOWERS") {

    def id = column[Int]("ID")
    def follower = column[Int]("FOLLOWER")

    def * = id ~ follower

    def followersPK = primaryKey("FOLLOWERS_PK", *)

    def profileFK = foreignKey("FOLLOWERS_PROFILE_FK", id, Profiles)(_.id)

    def followerFK = foreignKey("FOLLOWERS_FOLLOWER_FK", id, Profiles)(_.id)
  }

  object GroupMessages extends Table[data.Message]("GROUP_MESSAGES") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def author = column[Int]("AUTHOR")
    def recipient = column[Int]("RECIPIENT")
    def title = column[Option[String]]("TITLE")
    def content = column[Clob]("CONTENT")
    def parent = column[Int]("PARENT")

    def * = id ~ author ~ recipient ~ title ~ content ~ parent <>
      (data.Message, data.Message.unapply _)

    def forInsert = author ~ recipient ~ title ~ content ~ parent

    def authorFK =
      foreignKey("GROUP_MESSAGES_AUTHOR_FK", author, Profiles)(_.id)

    def recipientFK =
      foreignKey("GROUP_MESSAGES_RECIPIENT_FK", recipient, Groups)(_.id)

    def parentFK =
      foreignKey("GROUP_MESSAGES_PARENT_FK", parent, GroupMessages)(_.id)
  }

  /**
   * Performs an arbitrary method in a new database session.
   */
  def query[T](f: => T): T = db withSession f

  /**
   * Inserts a new profile into the database.
   */
  def insertProfile(
      passwordHash: String,
      name: String,
      phoneNumber: Option[String]): Int = {
    (Profiles.passwordHash ~
     Profiles.name ~
     Profiles.phoneNumber) returning Profiles.id insert(
       (passwordHash, name, phoneNumber))
  }

  /**
   * Inserts a new group into the database.
   */
  def insertGroup(
      name: String,
      owner: Int,
      description: String,
      privacy: data.GroupPrivacy,
      parent: Option[Int]): Int = {
    val clob = threadLocalSession.conn.createClob()
    clob.setString(1, description)
    Groups.forInsert returning Groups.id insert(
      (name, owner, clob, privacy, parent))
  }

  /**
   * Delete all session data for a specific user.
   */
  def deleteSessionData(id: Int) {
    Query(Sessions).filter(_.id === id).delete
  }

  /**
   * Deletes expired Sessions etc.
   */
  def deleteOldData() {
    val t = System.currentTimeMillis()
    Query(Sessions).filter(_.expirationTime <= t).delete
    Query(PasswordResetTokens).filter(_.expirationTime <= t).delete
    Query(EmailConfirmationTokens).filter(_.expirationTime <= t).delete
  }

  /**
   * Deletes all session data and updates the email address of a given user.
   */
  def updateEmail(user: data.Profile, email: Option[String]): Int = {
    deleteSessionData(user.id)
    Query(EmailConfirmationTokens).filter(_.id === user.id).delete
    Query(Profiles).filter(_.id === user.id).update(
      data.Profile(
        user.id,
        email,
        user.passwordHash,
        user.name,
        user.phoneNumber))
  }

  /**
   * Delete all session data of a specific user and update its password.
   */
  def updatePassword(user: data.Profile, password: String): Int = {
    deleteSessionData(user.id)
    Query(PasswordResetTokens).filter(_.id === user.id).delete
    Query(Profiles).filter(_.id === user.id).update(
      data.Profile(
        user.id,
        user.email,
        BCrypt.hashpw(password, BCrypt.gensalt()),
        user.name,
        user.phoneNumber))
  }

  /**
   * Queries a Profile by id.
   */
  def getProfile(id: Int): Option[data.Profile] =
    Query(Profiles).filter(_.id === id).take(1).list.headOption

  /**
   * Queries a Profile by email.
   */
  def getProfile(email: String): Option[data.Profile] =
    Query(Profiles).filter(_.email === email).take(1).list.headOption

  /**
   * Make two Profiles "friends" by making them follow each other.
   * Returns true on success.
   */
  def addFriends(id1: Int, id2: Int): Boolean = {
    Followers.insert((id1, id2)) + Followers.insert((id2, id1)) == 2
  }

  /**
   * Unfriend two Profiles by having them unfollow each other.
   * Returns true on success.
   */
  def removeFriends(id1: Int, id2: Int): Boolean = {
    Query(Followers).filter(q =>
      (q.id === id1 && q.follower === id2) ||
      (q.id === id2 && q.follower === id1)).delete == 2
  }
}

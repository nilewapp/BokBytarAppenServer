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

/**
 * Defines tables and provides database access
 */
trait DB {

  val DBName = "db"
  lazy val url = "jdbc:h2:" + getClass.getResource("/").getPath() + DBName
  lazy val db = Database.forURL(url, driver = "org.h2.Driver")

  case class Profile(
    id: String, 
    passwordHash: String,
    salt: String,
    name: String,
    phoneNumber: Option[String],
    university: String)

  case class Session(
    profile: String,
    series: String,
    token: String,
    expirationTime: Long)

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
    def id = column[String]("ID", O.PrimaryKey)
    def passwordHash = column[String]("PASSWORD_HASH")
    def salt = column[String]("SALT")
    def name = column[String]("NAME")
    def phoneNumber = column[Option[String]]("PHONE_NUMBER", O.Nullable)
    def university = column[String]("UNIVERSITY")
    def * = id ~ passwordHash ~ salt ~ name ~ phoneNumber ~ university <> (Profile, Profile.unapply _)
    def universityFK = foreignKey("UNIVERSITY_FK", university, Universities)(_.name)
  }

  object WantedBooks extends Table[(String, Int)]("WANTED_BOOKS") {
    def profileId = column[String]("PROFILE_ID")
    def isbn13 = column[Int]("ISBN")
    def * = profileId ~ isbn13
    def wantedBooksPK = primaryKey("WANTED_BOOKS_PK", profileId ~ isbn13)
    def profileFK = foreignKey("WANTED_PROFILE_FK", profileId, Profiles)(_.id)
  }

  object OwnedBooks extends Table[(String, Int)]("OWNED_BOOKS") {
    def profileId = column[String]("PROFILE_ID")
    def isbn13 = column[Int]("ISBN")
    def * = profileId ~ isbn13
    def ownedBooksPK = primaryKey("OWNED_BOOKS_PK", profileId ~ isbn13)
    def profileFK = foreignKey("OWNED_PROFILE_FK", profileId, Profiles)(_.id)
  }

  object Sessions extends Table[Session]("SESSION") {
    def profile = column[String]("PROFILE")
    def series = column[String]("SERIES")
    def token = column[String]("TOKEN")
    def expirationTime = column[Long]("EXPIRATION_TIME")
    def * = profile ~ series ~ token ~ expirationTime <> (Session, Session.unapply _)
    def sessionPK = primaryKey("SESSION_PK", profile ~ series ~ token)
    def profileFK = foreignKey("SESSION_PROFILE_FK", profile, Profiles)(_.id)
  }

  def all = Countries.ddl ++ 
    Cities.ddl ++ 
    Universities.ddl ++ 
    Profiles.ddl ++ 
    WantedBooks.ddl ++ 
    OwnedBooks.ddl ++
    Sessions.ddl

  def query[T](f: => T): T = db withSession f

  def drop = query {
    all.drop
  }

  def insertProfile(id: String, passwordHash: String, salt: String, name: String, phoneNumber: Option[String], university: String) {
    query {
      Profiles.insert(Profile(id, passwordHash, salt, name, phoneNumber, university))
    }
  }

  def init = query { 
    all.create
    
    Countries.insertAll(
      (752, "SE")
    , (826, "GB"))

    Cities.insertAll(
      ("STOCKHOLM", 752)
    , ("KARLSKRONA", 752)
    , ("GÖTEBORG", 752)
    , ("FILIPSTAD", 752)
    , ("FALUN", 752)
    , ("BORÅS", 752)
    , ("GÄVLE", 752)
    , ("HALMSTAD", 752)
    , ("JÖNKÖPING", 752)
    , ("SKÖVDE", 752)
    , ("KRISTIANSTAD", 752)
    , ("VISBY", 752)
    , ("TROLLHÄTTAN", 752)
    , ("UPPSALA", 752)
    , ("KARLSTAD", 752)
    , ("LINKÖPING", 752)
    , ("VÄXJÖ", 752)
    , ("LULEÅ", 752)
    , ("LUND", 752)
    , ("MALMÖ", 752)
    , ("SUNDSVALL", 752)
    , ("VÄSTERÅS", 752)
    , ("UMEÅ", 752)
    , ("ÖREBRO", 752)
    )

    Universities.insertAll(
      ("Beckmans Designhögskola", "STOCKHOLM")
    , ("Blekinge tekniska högskola", "KARLSKRONA")
    , ("Chalmers tekniska högskola", "GÖTEBORG")
    , ("Dans- och cirkushögskolan", "STOCKHOLM")
    , ("Ericastiftelsen", "STOCKHOLM")
    , ("Ersta Sköndal högskola", "STOCKHOLM")
    , ("Evidens AB", "GÖTEBORG")
    , ("Försvarshögskolan", "STOCKHOLM")
    , ("Gammelkroppa skogsskola", "FILIPSTAD")
    , ("Gymnastik- och idrottshögskolan", "STOCKHOLM")
    , ("Göteborgs universitet", "GÖTEBORG")
    , ("Handelshögskolan i Stockholm", "STOCKHOLM")
    , ("Högskolan Dalarna", "FALUN")
    , ("Högskolan i Borås", "BORÅS")
    , ("Högskolan i Gävle", "GÄVLE")
    , ("Högskolan i Halmstad", "HALMSTAD")
    , ("Högskolan i Jönköping", "JÖNKÖPING")
    , ("Högskolan i Skövde", "SKÖVDE")
    , ("Högskolan Kristianstad", "KRISTIANSTAD")
    , ("Högskolan på Gotland", "VISBY")
    , ("Högskolan Väst", "TROLLHÄTTAN")
    , ("Johannelunds teologiska högskola", "UPPSALA")
    , ("Karlstads universitet", "KARLSTAD")
    , ("Karolinska institutet", "STOCKHOLM")
    , ("Konstfack", "STOCKHOLM")
    , ("Kungl. Konsthögskolan", "STOCKHOLM")
    , ("Kungl. Musikhögskolan i Stockholm", "STOCKHOLM")
    , ("Kungl. Tekniska högskolan (KTH)", "STOCKHOLM")
    , ("Linköpings universitet", "LINKÖPING")
    , ("Linnéuniversitetet", "VÄXJÖ")
    , ("Luleå tekniska universitet", "LULEÅ")
    , ("Lunds universitet", "LUND")
    , ("Malmö högskola", "MALMÖ")
    , ("Mittuniversitetet", "SUNDSVALL")
    , ("Mälardalens högskola", "VÄSTERÅS")
    , ("Newmaninstitutet", "UPPSALA")
    , ("Operahögskolan i Stockholm", "STOCKHOLM")
    , ("Röda Korsets Högskola", "STOCKHOLM")
    , ("Sophiahemmet Högskola", "STOCKHOLM")
    , ("Stockholms Akademi för Psykoterapiutbildning", "STOCKHOLM")
    , ("Stockholms dramatiska högskola", "STOCKHOLM")
    , ("Stockholms Musikpedagogiska Institut", "STOCKHOLM")
    , ("Stockholms universitet", "STOCKHOLM")
    , ("Svenska Institutet för Kognitiv Psykoterapi", "STOCKHOLM")
    , ("Sveriges lantbruksuniversitet", "UPPSALA")
    , ("Södertörns högskola", "STOCKHOLM")
    , ("Teologiska Högskolan, Stockholm", "STOCKHOLM")
    , ("Umeå universitet", "UMEÅ")
    , ("Uppsala universitet", "UPPSALA")
    , ("Örebro Teologiska Högskola", "ÖREBRO")
    , ("Örebro universitet", "ÖREBRO"))
  }

}



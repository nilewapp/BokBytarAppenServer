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
 * Provides some utilities for creating and managing the database
 */
object DBManager extends DB {

  def all = Countries.ddl ++ 
    Cities.ddl ++ 
    Universities.ddl ++ 
    Profiles.ddl ++ 
    WantedBooks.ddl ++ 
    OwnedBooks.ddl ++
    Sessions.ddl

  def dropAll = query {
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



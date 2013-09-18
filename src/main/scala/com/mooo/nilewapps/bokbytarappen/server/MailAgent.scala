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

import javax.mail._
import javax.mail.internet._

import java.util.Properties

import com.typesafe.config._

/**
 * Defines a method to send emails.
 */
object MailAgent {

  /**
   * Sends an email to a given user with a given subject and content text 
   * using TLS. Uses application configuration to find sender credentials
   * and smtp options.
   */
  def send(to: String, subject: String, content: String) {

    val config = ConfigFactory.load().getConfig("mail")

    val from = config.getString("user")
    val pass = config.getString("pass")

    val props = new Properties()
    props.put("mail.smtp.auth", config.getString("auth"))
    props.put("mail.smtp.starttls.enable", config.getString("startttls-enable"))
    props.put("mail.smtp.host", config.getString("smtp-host"))
    props.put("mail.smtp.port", config.getString("port"))

    val session = Session.getInstance(props, new javax.mail.Authenticator() {
        override def getPasswordAuthentication() = new PasswordAuthentication(from, pass)
      })

    val mess= new MimeMessage(session)
    mess.setFrom(new InternetAddress(from))

    val recipients = InternetAddress.parse(to)
    if (recipients != null && recipients.length > 0) {
      mess.setRecipients(Message.RecipientType.TO, to)
    }

    mess.setSubject(subject)
    mess.setText(content)

    Transport.send(mess)
  }
}

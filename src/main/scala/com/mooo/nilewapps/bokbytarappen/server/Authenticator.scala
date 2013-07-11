package com.mooo.nilewapps.bokbytarappen.server

trait Authenticator[U] extends UserPassAuthenticator with DB {
  def authenticate(credentials: Option[(String, String)]): Future[Option[U]] = {
    
  }
}

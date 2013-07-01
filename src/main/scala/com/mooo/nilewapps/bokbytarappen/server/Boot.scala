package com.mooo.nilewapps.bokbytarappen.server

import spray.can.server.SprayCanHttpServerApp
import akka.actor.Props

/**
 *  Starts the server 
 */
object Boot extends App with SprayCanHttpServerApp with SslConfig {

  /* Create and start the service actor */
  val service = system.actorOf(Props[ServiceActor], "handler")

  /* Create and bind the http server */
  newHttpServer(service) ! Bind(interface = "0.0.0.0", port = 8443)

}

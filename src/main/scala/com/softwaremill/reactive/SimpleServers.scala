package com.softwaremill.reactive

import java.net.InetSocketAddress

import akka.actor.ActorSystem

trait SimpleServers {
  implicit val system = ActorSystem()
  val receiverAddress = new InetSocketAddress("localhost", 9182)
}

package com.softwaremill.reactive

import akka.actor.Actor
import scala.concurrent.duration._

class BytesPerSecondActor extends Actor {
  override def preStart() = {
    import context.dispatcher
    context.system.scheduler.schedule(1.second, 1.second, self, Tick)
  }

  private var bytes = 0

  override def receive = {
    case Tick =>
      println(s"Bytes/second: $bytes")
      bytes = 0
    case b: Int => bytes += b
  }
}

object Tick
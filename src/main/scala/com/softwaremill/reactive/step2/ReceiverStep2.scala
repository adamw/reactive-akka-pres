package com.softwaremill.reactive.step2

import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{Source, Sink, StreamTcp}
import com.softwaremill.reactive._
import com.softwaremill.reactive.complete.LargestDelayActorComplete

import scala.concurrent.duration._

/**
 * - sending data to an actor, which processes it further
 * - the actor must be reactive
 */
class ReceiverStep2(receiverAddress: InetSocketAddress)(implicit val system: ActorSystem) extends Logging {

  def run(): Unit = {
    implicit val mat = ActorFlowMaterializer()

    val largestDelayActor = system.actorOf(Props[LargestDelayActorComplete])

    logger.info("Receiver: binding to " + receiverAddress)
    StreamTcp().bind(receiverAddress).runForeach { conn =>
      logger.info(s"Receiver: sender connected (${conn.remoteAddress})")

      val receiveSink = conn.flow
        .transform(() => new ParseLinesStage("\n", 4000000))
        .filter(_.startsWith("20"))
        .map(_.split(","))
        .mapConcat(FlightData(_).toList)
        .to(Sink(ActorSubscriber[FlightData](largestDelayActor)))

      Source.empty.to(receiveSink).run()
    }

    import system.dispatcher
    system.scheduler.schedule(0.seconds, 1.second, largestDelayActor, LogLargestDelay)
  }
}

object ReceiverStep2 extends App {
  implicit val system = ActorSystem()
  new ReceiverStep2(new InetSocketAddress("localhost", 9182)).run()
}

package com.softwaremill.reactive.step1

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{ForeachSink, FutureSource, StreamTcp}
import com.softwaremill.reactive._

import scala.concurrent.Promise

/**
 * - flow from the client, transforming, no response
 * - *elastic*: delay to see the backpressure
 */
class ReceiverStep1(receiverAddress: InetSocketAddress)(implicit val system: ActorSystem) extends Logging {

  def run(): Unit = {
    implicit val mat = FlowMaterializer()

    logger.info("Receiver: binding to " + receiverAddress)
    StreamTcp().bind(receiverAddress).connections.foreach { conn =>
      logger.info(s"Receiver: sender connected (${conn.remoteAddress})")

      val receiveSink = conn.flow
        .transform(() => new ParseLinesStage("\n", 4000000))
        .filter(_.startsWith("20"))
        .map(_.split(","))
        .mapConcat(FlightData(_).toList)
        .to(ForeachSink { flightData =>
          logger.info("Got data: " + flightData)
          Thread.sleep(500L)
        })

      receiveSink.runWith(FutureSource(Promise().future))
    }
  }
}

object ReceiverStep1 extends App {
  implicit val system = ActorSystem()
  new ReceiverStep1(new InetSocketAddress("localhost", 9182)).run()
}

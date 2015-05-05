package com.softwaremill.reactive.step1

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import com.softwaremill.reactive._

/**
 * - flow from the client, transforming, no response
 * - *elastic*: delay to see the backpressure
 */
class ReceiverStep1(host: String, port: Int)(implicit val system: ActorSystem) extends Logging {

  def run(): Unit = {
    implicit val mat = ActorFlowMaterializer()

    logger.info(s"Receiver: binding to $host:$port")
    Tcp().bind(host, port).runForeach { conn =>
      logger.info(s"Receiver: sender connected (${conn.remoteAddress})")

      val receiveSink = conn.flow
        .transform(() => new ParseLinesStage("\n", 4000000))
        .filter(_.startsWith("20"))
        .map(_.split(","))
        .mapConcat(FlightData(_).toList)
        .to(Sink.foreach { flightData =>
          logger.info("Got data: " + flightData)
          Thread.sleep(100L)
        })

      Source.empty.to(receiveSink).run()
    }
  }
}

object ReceiverStep1 extends App {
  implicit val system = ActorSystem()
  new ReceiverStep1("localhost", 9182).run()
}

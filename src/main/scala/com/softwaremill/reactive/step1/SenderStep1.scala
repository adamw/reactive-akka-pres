package com.softwaremill.reactive.step1

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.softwaremill.reactive._

object SenderStep1 extends App with Logging {
  implicit val system = ActorSystem()
  val serverConnection = StreamTcp().outgoingConnection(new InetSocketAddress("localhost", 9182))

  val getLines = () => scala.io.Source.fromFile("/Users/adamw/projects/reactive-akka-pres/data/2008.csv").getLines()

  val linesSource = Source(getLines).map { line => ByteString(line + "\n") }
  val logCompleteSink = Sink.onComplete(r => logger.info("Completed with: " + r))

  val flow = linesSource
    .via(serverConnection.flow)
    .to(logCompleteSink)

  implicit val mat = FlowMaterializer()
  flow.run()
}

package com.softwaremill.reactive.lambdadays

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.softwaremill.reactive._

object Sender extends App with Logging {
  implicit val system = ActorSystem()
  val serverConnection = StreamTcp().outgoingConnection(new InetSocketAddress("localhost", 9182))

  val getLines = () => scala.io.Source.fromFile("/Users/adamw/projects/reactive-akka-pres/data/2008.csv").getLines()

  val linesSource = Source(getLines).map { line => ByteString(line + "\n") }
  val logCompleteSink = Sink.onComplete(r => logger.info("Completed with: " + r))

  val flow = linesSource
    .via(serverConnection)
    .to(logCompleteSink)

  implicit val mat = ActorFlowMaterializer()
  flow.run()
}


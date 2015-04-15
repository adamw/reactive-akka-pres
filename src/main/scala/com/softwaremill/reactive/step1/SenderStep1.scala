package com.softwaremill.reactive.step1

import java.net.InetSocketAddress

import akka.actor.{Props, ActorSystem}
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.softwaremill.reactive._

/**
 * - source: iterator from file (lazy iterator)
 * - sink: just log
 * - server connnection: req -> resp flow
 * - *message driven*: materializer: actors
 */
object SenderStep1 extends App with Logging {
  implicit val system = ActorSystem()
  val bytesPerSecondActor = system.actorOf(Props[BytesPerSecondActor])
  val serverConnection = StreamTcp().outgoingConnection(new InetSocketAddress("localhost", 9182))

  val getLines = () => scala.io.Source.fromFile("/Users/adamw/projects/reactive-akka-pres/data/2008.csv").getLines()

  val linesSource = Source(getLines).map { line =>
    bytesPerSecondActor ! line.length
    ByteString(line + "\n")
  }
  val logCompleteSink = Sink.onComplete(r => logger.info("Completed with: " + r))

  val flow = linesSource
    .via(serverConnection)
    .to(logCompleteSink)

  implicit val mat = ActorFlowMaterializer()
  flow.run()
}


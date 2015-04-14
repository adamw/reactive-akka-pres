package com.softwaremill.reactive.complete

import java.net.InetSocketAddress

import akka.actor.Actor.emptyBehavior
import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.ClusterSingletonManager
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{Source, Sink, StreamTcp}
import com.softwaremill.reactive._
import com.softwaremill.reactive.complete.ReceiverComplete.ReceiverClusterNode
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object ReceiverComplete {

  class Receiver(receiverAddress: InetSocketAddress)(implicit val system: ActorSystem) extends Logging {

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

        receiveSink.runWith(Source.empty)
      }

      import system.dispatcher
      system.scheduler.schedule(0.seconds, 1.second, largestDelayActor, LogLargestDelay)
    }
  }

  class ReceiverClusterNode(clusterPort: Int) {
    def run(): Unit = {
      val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$clusterPort")
        .withFallback(ConfigFactory.load("cluster-receiver-template"))

      val system = ActorSystem("receiver", conf)

      system.actorOf(ClusterSingletonManager.props(
        singletonProps = Props(classOf[ReceiverNodeActor], clusterPort),
        singletonName = "receiver",
        terminationMessage = PoisonPill,
        role = Some("receiver")),
        name = "receiver-manager")
    }
  }

  class ReceiverNodeActor(clusterPort: Int) extends Actor {
    val receiverAddress = new InetSocketAddress("localhost", clusterPort + 10)

    override def preStart() = {
      super.preStart()
      new Receiver(receiverAddress)(context.system).run()
    }

    override def receive = emptyBehavior
  }
}

object ClusteredReceiver1 extends App {
  new ReceiverClusterNode(9171).run()
}

object ClusteredReceiver2 extends App {
  new ReceiverClusterNode(9172).run()
}

object ClusteredReceiver3 extends App {
  new ReceiverClusterNode(9173).run()
}

object SimpleReceiver extends App {
  implicit val system = ActorSystem()
  new ReceiverComplete.Receiver(new InetSocketAddress("localhost", 9182)).run()
}
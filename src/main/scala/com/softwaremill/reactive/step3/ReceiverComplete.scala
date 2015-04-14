package com.softwaremill.reactive.step3

import java.net.InetSocketAddress

import akka.actor.Actor.emptyBehavior
import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.ClusterSingletonManager
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorSubscriber
import akka.stream.scaladsl.{Source, Sink, StreamTcp}
import com.softwaremill.reactive._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
 * - no changes
 */
class ReceiverStep3(receiverAddress: InetSocketAddress)(implicit val system: ActorSystem) extends Logging {

  def run(): Unit = {
    implicit val mat = ActorFlowMaterializer()

    val largestDelayActor = system.actorOf(Props[LargestDelayActorStep3])

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

object ReceiverStep3 extends App {
  implicit val system = ActorSystem()
  new ReceiverStep3(new InetSocketAddress("localhost", 9182)).run()
}

/**
 * - *resilient*
 * - starting a cluster singleton
 * - needs an actor which will be started/stopped
 * - actor: starts the receiver
 * - three apps to start the nodes
 */
class ReceiverClusterNodeStep3(clusterPort: Int) {
  def run(): Unit = {
    val conf = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$clusterPort")
      .withFallback(ConfigFactory.load("cluster-receiver-template"))

    val system = ActorSystem("receiver", conf)

    system.actorOf(ClusterSingletonManager.props(
      singletonProps = Props(classOf[ReceiverNodeActorStep3], clusterPort),
      singletonName = "receiver",
      terminationMessage = PoisonPill,
      role = Some("receiver")),
      name = "receiver-manager")
  }
}

class ReceiverNodeActorStep3(clusterPort: Int) extends Actor {
  val receiverAddress = new InetSocketAddress("localhost", clusterPort + 10)

  override def preStart() = {
    super.preStart()
    new ReceiverStep3(receiverAddress)(context.system).run()
  }

  override def receive = emptyBehavior
}

object ClusteredReceiver1Step3 extends App {
  new ReceiverClusterNodeStep3(9171).run()
}

object ClusteredReceiver2Step3 extends App {
  new ReceiverClusterNodeStep3(9172).run()
}

object ClusteredReceiver3Step3 extends App {
  new ReceiverClusterNodeStep3(9173).run()
}

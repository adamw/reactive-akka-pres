package com.softwaremill.reactive.step2

import akka.actor.Actor
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, MaxInFlightRequestStrategy}
import com.softwaremill.reactive._

/**
 * - actor subscriber: must handle OnNext()
 * - counting how many messages are in-flight
 * - specifying the request strategy
 */
class LargestDelayActorStep2 extends Actor with ActorSubscriber with Logging {
  private var largestDelay: Option[FlightWithDelayPerMile] = None

  private var inFlight = 0

  override protected def requestStrategy = new MaxInFlightRequestStrategy(10) {
    override def inFlightInternally = inFlight
  }

  def receive = {
    case OnNext(data: FlightData) =>
      FlightWithDelayPerMile(data).foreach { d =>
        inFlight += 1
        processDelayData(d)
        inFlight -= 1
      }
    case LogLargestDelay => logger.info("Largest delay so far: " + largestDelay)
  }

  def processDelayData(d: FlightWithDelayPerMile): Unit = {
    largestDelay = Some((d :: largestDelay.toList).max)
  }
}


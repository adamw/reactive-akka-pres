package com.softwaremill.reactive.step3

import akka.persistence.PersistentActor
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, MaxInFlightRequestStrategy}
import com.softwaremill.reactive._

/**
 * - *resilient*
 * - adding persistence: message is handled only when persist async completes
 * - handler for recovery
 * - extending persistent actor
 */
class LargestDelayActorStep3 extends PersistentActor with ActorSubscriber with Logging {
   private var largestDelay: Option[FlightWithDelayPerMile] = None

   override def persistenceId = "flight-actor"

   private var inFlight = 0

   override protected def requestStrategy = new MaxInFlightRequestStrategy(10) {
     override def inFlightInternally = inFlight
   }

   def receiveCommand = {
     case OnNext(data: FlightData) =>
        FlightWithDelayPerMile(data).foreach { d =>
          inFlight += 1
          persistAsync(d) { _ =>
            processDelayData(d)
            inFlight -= 1
          }
        }
     case LogLargestDelay => logger.info("Largest delay so far: " + largestDelay)
   }

   def receiveRecover = {
     case d: FlightWithDelayPerMile => processDelayData(d)
   }

   def processDelayData(d: FlightWithDelayPerMile): Unit = {
     largestDelay = Some((d :: largestDelay.toList).max)
   }
 }


package com.softwaremill.reactive

case class FlightData(from: String, to: String, distanceMiles: Int, delayMinutes: Int)

object FlightData {
  def apply(fields: Array[String]): Option[FlightData] = {
    try {
      Some(FlightData(fields(16), fields(17), fields(18).toInt, fields(14).toInt))
    } catch {
      case _: Exception => None
    }
  }
}

case class FlightWithDelayPerMile(flight: FlightData, delayPerMile: Double) extends Comparable[FlightWithDelayPerMile] {
  override def compareTo(o: FlightWithDelayPerMile) = delayPerMile.compareTo(o.delayPerMile)
}

object FlightWithDelayPerMile {
  def apply(data: FlightData): Option[FlightWithDelayPerMile] = {
    if (data.delayMinutes > 0) {
      Some(FlightWithDelayPerMile(data, 60.0d * data.delayMinutes / data.distanceMiles))
    } else None
  }
}

object LogLargestDelay

object GetReceiverAddress
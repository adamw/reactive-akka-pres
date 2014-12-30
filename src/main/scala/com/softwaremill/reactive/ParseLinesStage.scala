package com.softwaremill.reactive

import akka.stream.stage.{Directive, Context, StatefulStage}
import akka.util.ByteString

import scala.annotation.tailrec

/**
 * From http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/stream-cookbook.html
 */
class ParseLinesStage(separator: String, maximumLineBytes: Int) extends StatefulStage[ByteString, String] {
  private val separatorBytes = ByteString(separator)
  private val firstSeparatorByte = separatorBytes.head
  private var buffer = ByteString.empty
  private var nextPossibleMatch = 0

  def initial = new State {
    override def onPush(chunk: ByteString, ctx: Context[String]): Directive = {
      buffer ++= chunk
      if (buffer.size > maximumLineBytes) {
        println("XXX FAIL")
        ctx.fail(new IllegalStateException(s"Read ${buffer.size} bytes " +
          s"which is more than $maximumLineBytes without seeing a line terminator"))
      } else emit(doParse(Vector.empty).iterator, ctx)
    }

    @tailrec
    private def doParse(parsedLinesSoFar: Vector[String]): Vector[String] = {
      val possibleMatchPos = buffer.indexOf(firstSeparatorByte, from = nextPossibleMatch)
      if (possibleMatchPos == -1) {
        // No matching character, we need to accumulate more bytes into the buffer
        nextPossibleMatch = buffer.size
        parsedLinesSoFar
      } else {
        if (possibleMatchPos + separatorBytes.size > buffer.size) {
          // We have found a possible match (we found the first character of the terminator
          // sequence) but we don't have yet enough bytes. We remember the position to
          // retry from next time.
          nextPossibleMatch = possibleMatchPos
          parsedLinesSoFar
        } else {
          if (buffer.slice(possibleMatchPos, possibleMatchPos + separatorBytes.size)
            == separatorBytes) {
            // Found a match
            val parsedLine = buffer.slice(0, possibleMatchPos).utf8String
            buffer = buffer.drop(possibleMatchPos + separatorBytes.size)
            nextPossibleMatch -= possibleMatchPos + separatorBytes.size
            doParse(parsedLinesSoFar :+ parsedLine)
          } else {
            nextPossibleMatch += 1
            doParse(parsedLinesSoFar)
          }
        }
      }

    }
  }
}

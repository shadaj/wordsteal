package me.shadaj.wordsteal

import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import android.app.Activity

object WordDataParser {
  def parseData(source: Source, addMethod: ((String, Set[String])) => Unit)
               (percentage: Double, onLoad: => Unit): Unit = {
    val lines = source.getLines
    val numberOfLines = lines.next.toInt
    val linesToLoadToStart = numberOfLines * percentage

    lines.zipWithIndex.foreach { case (s, linesSoFar) =>
      val split = s.split('|')
      addMethod(((split(0), split.tail.toSet)))
      if (linesSoFar >= linesToLoadToStart) {
        onLoad
      }
    }
    
    source.close
  }
}

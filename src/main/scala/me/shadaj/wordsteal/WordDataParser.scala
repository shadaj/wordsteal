package me.shadaj.wordsteal

import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import android.app.Activity

object WordDataParser {
  def parseData(source: Source, dictionary: collection.mutable.ArrayBuffer[(String, Set[String])])
               (percentage: Double, onLoad: => Unit): Unit = {
    val lines = source.getLines
    val numberOfLines = lines.next.toInt
    val linesToLoadToStart = numberOfLines * percentage

    lines.zipWithIndex.foreach { case (s, linesSoFar) =>
      val split = s.split('|')
      dictionary += ((split(0), split.tail.toSet))
      if (linesSoFar >= linesToLoadToStart) {
        onLoad
      }
    }
    
    source.close
  }
}

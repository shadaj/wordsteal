package me.shadaj.wordsteal

import java.io.InputStreamReader
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import scala.io.Source
import android.view.View.OnKeyListener
import android.view.KeyEvent

class WordStealActivity extends Activity {
  var currentPoints = 0

  var words: List[String] = null

  val A = 'a'.toInt
  val Z = 'z'.toInt
  val alphabetSize = Z - A + 1

  def randomChar = ((math.random * alphabetSize) + A).toInt.toChar

  lazy val charactersDisplay = findViewById(R.id.characterDisplay).asInstanceOf[TextView]
  lazy val input = findViewById(R.id.input).asInstanceOf[EditText]
  lazy val response = findViewById(R.id.response).asInstanceOf[TextView]
  var currentStart = randomChar
  var currentEnd = randomChar

  def newLetters {
    currentStart = randomChar
    currentEnd = randomChar
    if (words.count(s => s.head.toLower == currentStart && s.last.toLower == currentEnd) >= 100) {
      charactersDisplay.setText(currentStart + " " + currentEnd)
    } else {
      newLetters
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)
    val assets = getAssets()
    val en_US = Source.fromInputStream(assets.open("en_US.dic"))

    words = en_US.getLines.toList

    newLetters
    
    input.setOnKeyListener {new OnKeyListener () {
      def onKey(v: View, keyCode: Int, event: KeyEvent) = {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          checkWord(v)
          true
        } else {
          false
        }
      }
    }}
  }

  def checkWord(view: View) {
    val inputWord = input.getText().toString()
    if (!(inputWord.length == 0)) {
      val toSet = (inputWord.head.toLower == currentStart && inputWord.last.toLower == currentEnd && words.exists(s => s.toLowerCase() == inputWord.toLowerCase())) match {
        case true => "Correct!"
        case false => "Incorrect :("
      }

      input.setText("")

      currentPoints += inputWord.length()

      response.setText(toSet)
    }
    
    newLetters
  }
}
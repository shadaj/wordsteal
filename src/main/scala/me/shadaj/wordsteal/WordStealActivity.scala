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
import android.widget.Button
import android.util.Log
import android.text.method.KeyListener
import android.text.Editable
import android.text.TextWatcher
import scala.annotation.tailrec
import android.opengl.Visibility

class WordStealActivity extends TypedActivity {
  lazy val charactersDisplay = findView(TR.characterDisplay)
  lazy val input = findView(TR.input)
  lazy val response = findView(TR.response)
  lazy val checkButton = findView(TR.checkButton)
  lazy val points = findView(TR.points)

  lazy val assets = getAssets()
  lazy val en_US = Source.fromInputStream(assets.open("en_US.dic"))
  lazy val words = en_US.getLines.map(_.toLowerCase).toSet
  
  val A = 'a'.toInt
  val Z = 'z'.toInt
  val alphabetSize = Z - A + 1
  
  def randomChar = ((math.random * alphabetSize) + A).toChar

  var currentStart = randomChar
  var currentEnd = randomChar
  var currentPoints = 0

  @tailrec
  private def newLetters {
    currentStart = randomChar
    currentEnd = randomChar
    if (words.count(s => s.head == currentStart && s.last == currentEnd) >= 100) {
      charactersDisplay.setText(s"$currentStart...$currentEnd")
    } else {
      newLetters
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main)

    newLetters
    
    val inputWatcher = new TextWatcher() {
      def afterTextChanged(s: Editable) {}
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val inputWord = s.toString().toLowerCase
        if (inputWord.length == 0) {
          checkButton.setEnabled(false)
        } else if (inputWord.head == currentStart && inputWord.last == currentEnd) {
          checkButton.setEnabled(true)
        } else {
          checkButton.setEnabled(false)
        }
      }
    }

    input.addTextChangedListener(inputWatcher)
    
    input.setOnKeyListener {
      new OnKeyListener() {
        def onKey(v: View, keyCode: Int, event: KeyEvent) = {
          if (keyCode == KeyEvent.KEYCODE_ENTER) {
            checkWord(v)
            true
          } else {
            false
          }
        }
      }
    }
    
    en_US.close
    assets.close
  }

  def checkWord(view: View) {
    val inputWord = input.getText().toString().toLowerCase
    if (!(inputWord.length == 0)) {
      val correct = words.exists(_ == inputWord)
      val responseText = correct match {
        case true => "Correct!"
        case false => "Incorrect :("
      }

      input.setText("")

      if (correct) {
        currentPoints += inputWord.length()*100
        points.setText("Points: " + currentPoints)
      }

      response.setText(responseText)
      
      newLetters
    }
  }
}
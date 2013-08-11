package me.shadaj.wordsteal

import scala.annotation.tailrec
import scala.io.Source

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.View.OnKeyListener
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class WordStealActivity extends Activity {
  lazy val charactersDisplay = findViewById(R.id.characterDisplay).asInstanceOf[TextView]
  lazy val input = findViewById(R.id.input).asInstanceOf[EditText]
  lazy val response = findViewById(R.id.response).asInstanceOf[TextView]
  lazy val checkButton = findViewById(R.id.checkButton).asInstanceOf[Button]
  lazy val points = findViewById(R.id.points).asInstanceOf[TextView]

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

  var lives = 3
  
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
    setContentView(R.layout.game)

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
  }

  def checkWord(view: View) {
    val inputWord = input.getText().toString().toLowerCase
    if (!(inputWord.length <= 1)) {
      val correct = words.contains(inputWord)
      val responseText = correct match {
        case true => "Correct!"
        case false => "Incorrect :("
      }

      input.setText("")

      if (correct) {
        currentPoints += inputWord.length()*100
        points.setText("Points: " + currentPoints)
      } else {
        lives -= 1
        
        if (lives == 0) {
          this.setContentView(R.layout.gameover)
        }
      }

      response.setText(responseText)
      
      newLetters
    }
  }
}
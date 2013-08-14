package me.shadaj.wordsteal

import scala.io.Source

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.View.OnKeyListener
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView

class WordStealActivity extends Activity {
  def charactersDisplay = findViewById(R.id.characterDisplay).asInstanceOf[TextView]
  def input = findViewById(R.id.input).asInstanceOf[EditText]
  def response = findViewById(R.id.response).asInstanceOf[TextView]
  def checkButton = findViewById(R.id.checkButton).asInstanceOf[Button]
  def points = findViewById(R.id.points).asInstanceOf[TextView]
  def livesWidget = findViewById(R.id.lives).asInstanceOf[TextView]
  def loadingProgress = findViewById(R.id.loadingProgress).asInstanceOf[ProgressBar]
  def highScores = findViewById(R.id.highScores).asInstanceOf[TextView]

  val PERCENTAGE_TO_LOAD = 0.01
  val MAX_LIVES = 5

  val processedWords = collection.mutable.ArrayBuffer[(String, Set[String])]()

  def processWords = {
    var showingLoadingScreen = true
    var linesSoFar = 0

    val dict = Source.fromInputStream(getAssets.open("web2.wordDic"))
    val lines = dict.getLines()
    val numberOfLines = lines.next.toInt
    val linesToLoadToStart = numberOfLines * PERCENTAGE_TO_LOAD;
    
    val updateProgressBar = new Runnable {
      override def run {
        if (showingLoadingScreen) {
          loadingProgress.setProgress(((linesSoFar / linesToLoadToStart) * 100).toInt)
        }
      }
    }

    lines.foreach { s =>
      val split = s.split(' ')
      processedWords += ((split(0), split.tail.toSet))
      if (showingLoadingScreen && linesSoFar >= linesToLoadToStart) {
        showingLoadingScreen = false
        val startGameRunnable = new Runnable {
          override def run() {
            startGame
          }
        }
        runOnUiThread(startGameRunnable)
      } else {
        runOnUiThread(updateProgressBar)
        linesSoFar += 1
      }
    }
    dict.close
  }

  var currentPoints = 0
  var lives = MAX_LIVES

  var index = 0

  def currentStart = processedWords(index)._1.head
  def currentEnd = processedWords(index)._1.last

  def newLetters {
    index = (math.random * processedWords.size).toInt
    charactersDisplay.setText(currentStart + "..." + currentEnd)
  }

  def addInputWatchers {
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
  }

  def startGame {
    setContentView(R.layout.game)
    newLetters
    addInputWatchers
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.loading)

    val loadThread = new Thread {
      override def run() {
        processWords
      }
    }

    loadThread.start()
  }

  def gameOver {
    val pref = getPreferences(Context.MODE_PRIVATE)
    val previousScores = pref.getString("highscores", "-1").split(" ").toList.map(_.toInt).filter(_ >= 0)
    val newScores = (currentPoints :: previousScores).sorted.reverse
    val editor = pref.edit()
    editor.putString("highscores", newScores.take(10).mkString(" "))
    editor.commit()

    val thisGameIndex = newScores.indexOf(currentPoints)
    val thisGameText = "THIS GAME: " + newScores(thisGameIndex)
    val thisGameDisplay = newScores.map(_.toString).updated(thisGameIndex, thisGameText).mkString("\n")

    val thisGameColored = new SpannableStringBuilder(thisGameDisplay)
    val thisGameStart = thisGameDisplay.indexOf('T')
    val thisGameEnd = thisGameStart + thisGameText.length
    thisGameColored.setSpan(new ForegroundColorSpan(android.graphics.Color.GREEN), thisGameStart, thisGameEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

    setContentView(R.layout.gameover)
    highScores.setText(thisGameColored)
  }

  def checkWord(view: View) {
    val inputWord = input.getText.toString.toLowerCase
    if (!(inputWord.length <= 1)) {
      val correct = processedWords(index)._2.contains(inputWord.tail.init)
      val responseText = correct match {
        case true => "Correct!"
        case false => "Incorrect :("
      }

      input.setText("")

      if (correct) {
        currentPoints += inputWord.length() * 100
        points.setText("Points: " + currentPoints)
        response.setTextColor(android.graphics.Color.GREEN)
      }

      response.setText(responseText)
      newLetters

      if (!correct) {
        response.setTextColor(android.graphics.Color.RED)
        lives -= 1

        if (lives == 0) {
          gameOver
        } else {
          livesWidget.setText("Lives: " + lives)
        }
      }
    }
  }

  def skipWord(view: View) {
    lives -= 1

    if (lives == 0) {
      gameOver
    } else {
      newLetters
      response.setTextColor(android.graphics.Color.YELLOW)
      response.setText("Skipped")
      livesWidget.setText("Lives: " + lives)
      input.setText("")
    }
  }

  def reset(view: View) {
    currentPoints = 0
    lives = MAX_LIVES
    setContentView(R.layout.game)
    newLetters

    addInputWatchers
  }
}
package me.shadaj.wordsteal

import scala.io.Source
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.{ Editable, SpannableStringBuilder, Spanned, TextWatcher, style }
import style.ForegroundColorSpan
import android.view.{ KeyEvent, View, ViewGroup, animation }
import ViewGroup.LayoutParams
import LayoutParams.{ MATCH_PARENT, WRAP_CONTENT }
import View.OnKeyListener
import animation.AnimationUtils
import android.widget.{ Button, EditText, ProgressBar, TextSwitcher, TextView }
import android.util.TypedValue
import android.content.Intent
import java.util.Timer
import java.util.TimerTask
import com.google.example.games.basegameutils.BaseGameActivity
import com.google.android.gms.games.GamesClient
import android.hardware.input.InputManager
import android.view.inputmethod.InputMethodManager
import android.content.SharedPreferences

class WordStealActivity extends BaseGameActivity with View.OnClickListener {
  def charactersDisplay: TextSwitcher = findViewById(R.id.characterDisplay).asInstanceOf[TextSwitcher]
  def input: EditText = findViewById(R.id.input).asInstanceOf[EditText]
  def response: TextView = findViewById(R.id.response).asInstanceOf[TextView]
  def checkButton: Button = findViewById(R.id.checkButton).asInstanceOf[Button]
  def points: TextView = findViewById(R.id.points).asInstanceOf[TextView]
  def livesWidget: TextView = findViewById(R.id.lives).asInstanceOf[TextView]
  def highScores: List[TextView] = List(R.id.highScore0,
    R.id.highScore1,
    R.id.highScore2,
    R.id.highScore3,
    R.id.highScore4,
    R.id.highScore5).map(id => findViewById(id).asInstanceOf[TextView])
  def timeView: TextView = findViewById(R.id.timeViewer).asInstanceOf[TextView]

  def gamesClient: GamesClient = mHelper.getGamesClient

  val PERCENTAGE_TO_LOAD = 0.01
  val MAX_LIVES = 5
  val CHARACTER_DISPLAY_FONT_SIZE = 48
  val HIGH_SCORE_MEMORY = 5
  val START_SECONDS = 60
  val WORDSTEAL_LINK = "https://play.google.com/store/apps/details?id=me.shadaj.wordsteal"

  lazy val pref: SharedPreferences = getSharedPreferences("WordStealActivity", Context.MODE_PRIVATE)

  def loadShowHowToPlay: Unit = {
    if (currentScreen == LOADING) {
      val startGameRunnable = new Runnable {
        override def run() {
          currentScreen = HOW_TO_PLAY
          setContentView(R.layout.howtoplay)
        }
      }
      runOnUiThread(startGameRunnable)
    }
  }

  def loadShowMain: Unit = {
    if (currentScreen == LOADING) {
      val startGameRunnable = new Runnable {
        override def run() {
          currentScreen = MAIN_MENU
          showMain
        }
      }
      runOnUiThread(startGameRunnable)
    }
  }

  val processedWords = new collection.mutable.ArrayBuffer[(String, Set[String])]()

  var signedIn = false

  val LOADING = 0
  val HOW_TO_PLAY = 1
  val MAIN_MENU = 2
  val GAME = 3
  val GAMEOVER = 4
  var currentScreen = LOADING

  def addToDictionary(value: (String, Set[String])): Unit = {
    processedWords += value
  }

  def processWords: Unit = {
    val dict = Source.fromInputStream(getAssets.open("2of12.wordDic"))
    val loadThread = new Thread {
      override def run(): Unit = {
        WordDataParser.parseData(dict, addToDictionary)(PERCENTAGE_TO_LOAD, loadShowMain)
      }
    }

    loadThread.start
  }

  var currentPoints = 0
  var lives = MAX_LIVES

  var index = 0

  var secondsRemaining = START_SECONDS
  var secondsTimer: Timer = null

  def reduceTime: Unit = {
    try {
      secondsRemaining -= 1
      timeView.setText(secondsRemaining.toString)
      if (secondsRemaining == 0) {
        gameOver
      }
    } catch {
      case _ =>
    }
  }

  def currentStart: Char = processedWords(index)._1.head
  def currentEnd: Char = processedWords(index)._1.last

  def newLetters: Unit = {
    index = (math.random * processedWords.size).toInt
    charactersDisplay.setText(currentStart + "..." + currentEnd)
  }

  def addInputWatchers: Unit = {
    val inputWatcher = new TextWatcher {
      def afterTextChanged(s: Editable) {}
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (checkButton != null) {
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
    }

    input.addTextChangedListener(inputWatcher)

    input.setOnKeyListener {
      new OnKeyListener {
        def onKey(v: View, keyCode: Int, event: KeyEvent): Boolean = {
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

  def startGame: Unit = {
    lives = MAX_LIVES
    setContentView(R.layout.game)
    def styledTextView: TextView = {
      val textView = new TextView(this)
      textView.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT))
      textView.setGravity(android.view.Gravity.CENTER_HORIZONTAL)
      textView.setTextAppearance(this, android.R.style.TextAppearance_Large)
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, CHARACTER_DISPLAY_FONT_SIZE)
      textView
    }
    charactersDisplay.addView(styledTextView)
    charactersDisplay.addView(styledTextView)
    newLetters
    addInputWatchers

    secondsRemaining = START_SECONDS
    secondsTimer = new Timer
    secondsTimer.scheduleAtFixedRate(new TimerTask {
      def run(): Unit = {
        runOnUiThread(new Runnable {
          def run(): Unit = {
            reduceTime
          }
        })
      }
    }, 0, 1000)
  }

  def startGame(view: View): Unit = {
    startGame
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.loading)
    processWords
  }

  def gameOver: Unit = {
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
    inputManager.hideSoftInputFromWindow(input.getWindowToken(), 0)
    val previousScores = pref.getString("highscores", "-1").split(" ").toList.map(_.toInt).filter(_ >= 0)
    val newScores = (currentPoints :: previousScores).sorted.reverse
    val editor = pref.edit
    editor.putString("highscores", newScores.take(HIGH_SCORE_MEMORY).mkString(" "))
    editor.commit

    val thisGameIndex = newScores.indexOf(currentPoints)

    setContentView(R.layout.gameover)
    val views = highScores
    newScores.take(HIGH_SCORE_MEMORY + 1).zipWithIndex.foreach {
      case (text, index) =>
        views(index).setText(text.toString)
        if (index == thisGameIndex) {
          views(index).setTextColor(android.graphics.Color.GREEN)
          val animation = AnimationUtils.loadAnimation(this, R.anim.bouncing)
          views(index).startAnimation(animation)
        }
    }

    secondsTimer.cancel()

    val currentPointsSoFar = pref.getInt("pointsSoFar", 0)

    if (signedIn) {
      if (!pref.contains("pointsSoFar")) {
        gamesClient.unlockAchievement(getString(R.string.gettingStarted))
      }

      val difference = ((currentPointsSoFar + currentPoints) / 1000) - (currentPointsSoFar / 1000)
      if (difference >= 1) {
        gamesClient.incrementAchievement(getString(R.string.gettingThis), difference)
        gamesClient.incrementAchievement(getString(R.string.novice), difference)
        gamesClient.incrementAchievement(getString(R.string.intermediate), difference)
        gamesClient.incrementAchievement(getString(R.string.advanced), difference)
        gamesClient.incrementAchievement(getString(R.string.millionaire), difference)
      }

      gamesClient.submitScore(getString(R.string.highScoreLeaderboard), newScores.head)
    }

    editor.putInt("pointsSoFar", currentPointsSoFar + currentPoints)
  }

  def checkWord(view: View): Unit = {
    val inputWord = input.getText.toString.toLowerCase
    if (!(inputWord.length <= 1)) {
      val correct = processedWords(index)._2.contains(inputWord.tail.init)
      val responseText = correct match {
        case true => "Correct!"
        case false => "Incorrect :("
      }

      input.setText("")

      if (correct) {
        if (signedIn && inputWord.length == 10) {
          gamesClient.unlockAchievement(getString(R.string.perfect10))
        }
        currentPoints += inputWord.length() * 100
        points.setText("Points: " + currentPoints)
        response.setTextColor(android.graphics.Color.GREEN)
        secondsRemaining += inputWord.length
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

  def skipWord(view: View): Unit = {
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

  def reset(view: View): Unit = {
    currentPoints = 0
    lives = MAX_LIVES
    startGame
  }

  def brag(view: View): Unit = {
    val shareIntent = new Intent(android.content.Intent.ACTION_SEND)
    shareIntent.setType("text/plain")
    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, s"I scored $currentPoints on Wordsteal!")
    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
      s"I just scored $currentPoints on Wordsteal! Do you think that you can beat me? $WORDSTEAL_LINK")
    startActivity(Intent.createChooser(shareIntent, "Brag to your friends!"))
  }

  def showHowToPlay(view: View): Unit = {
    setContentView(R.layout.howtoplay)
  }

  def onSignInFailed: Unit = {

  }

  def onSignInSucceeded: Unit = {
    signedIn = true
    if (currentScreen == MAIN_MENU) {
      try {
        findViewById(R.id.sign_in_bar).setVisibility(View.GONE)
        findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE)

        findViewById(R.id.viewAchievements).setVisibility(View.VISIBLE)
        findViewById(R.id.viewLeaderboards).setVisibility(View.VISIBLE)
      } catch {
        case _ =>
      }
    }
  }

  override def onClick(view: View): Unit = {
    if (view.getId == R.id.sign_in_button) {
      beginUserInitiatedSignIn
    }
  }

  def signOutGame(view: View): Unit = {
    signedIn = false
    signOut()

    findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE)
    findViewById(R.id.sign_out_bar).setVisibility(View.GONE)

    findViewById(R.id.viewAchievements).setVisibility(View.GONE)
    findViewById(R.id.viewLeaderboards).setVisibility(View.GONE)
  }

  def showMain: Unit = {
    currentScreen = MAIN_MENU
    setContentView(R.layout.main)
    findViewById(R.id.sign_in_button).setOnClickListener(this)
    if (signedIn) {
      findViewById(R.id.sign_in_bar).setVisibility(View.GONE)
      findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE)
      findViewById(R.id.viewAchievements).setVisibility(View.VISIBLE)
      findViewById(R.id.viewLeaderboards).setVisibility(View.VISIBLE)
    }
  }

  def showMain(view: View): Unit = {
    showMain
  }

  def showAchieve(view: View): Unit = {
    startActivityForResult(gamesClient.getAchievementsIntent, 1)
  }

  def showLeader(view: View): Unit = {
    startActivityForResult(gamesClient.getLeaderboardIntent(getString(R.string.highScoreLeaderboard)), 1)
  }
}

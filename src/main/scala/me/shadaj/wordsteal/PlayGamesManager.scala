package me.shadaj.wordsteal

import com.google.android.gms.games.GamesClient
import android.content.Context

object PlayGamesManager {
  def incrementTotalPointsAchievement(context: Context, gamesClient: GamesClient, difference: Int): Unit = {
    import context._
    gamesClient.incrementAchievement(getString(R.string.gettingThis), difference)
    gamesClient.incrementAchievement(getString(R.string.novice), difference)
    gamesClient.incrementAchievement(getString(R.string.intermediate), difference)
    gamesClient.incrementAchievement(getString(R.string.advanced), difference)
    gamesClient.incrementAchievement(getString(R.string.millionaire), difference)
  }
}

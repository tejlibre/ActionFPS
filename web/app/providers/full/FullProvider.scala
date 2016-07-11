package providers.full

import java.time.YearMonth

import akka.agent.Agent
import com.actionfps.accumulation.{FullIterator, FullProfile, HOF}
import com.actionfps.clans.Clanwars
import com.actionfps.gameparser.enrichers.JsonGame
import com.actionfps.players.PlayersStats
import com.actionfps.stats.Clanstats
import com.google.inject.ImplementedBy

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FullProviderImpl])
abstract class FullProvider()(implicit executionContext: ExecutionContext) {


  protected[providers] def fullStuff: Future[Agent[FullIterator]]

  def getRecent(n: Int) =
    fullStuff.map(_.get().recentGames(n))

  def events: Future[List[Map[String, String]]] = {
    fullStuff.map(_.get().events)
  }

  def clanwars: Future[Clanwars] = {
    fullStuff.map(_.get().clanwars)
  }

  def playerRanks: Future[PlayersStats] = {
    fullStuff.map(_.get().playersStats)
  }

  def playerRanksOverTime: Future[Map[YearMonth, PlayersStats]] = {
    fullStuff.map(_.get().playersStatsOverTime)
  }

  def clanstats: Future[Clanstats] = {
    fullStuff.map(_.get().clanstats)
  }

  def hof: Future[HOF] = {
    fullStuff.map(_.get().hof)
  }

  def allGames: Future[List[JsonGame]] = {
    fullStuff.map(_.get().games.values.toList.sortBy(_.id))
  }

  def game(id: String): Future[Option[JsonGame]] = {
    fullStuff.map(_.get().games.get(id))
  }

  def getPlayerProfileFor(id: String): Future[Option[FullProfile]] = {
    fullStuff.map(_.get()).map(_.getProfileFor(id))
  }

  def reloadReference(): Future[FullIterator]

}

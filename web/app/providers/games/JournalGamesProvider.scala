package providers.games

/**
  * Created by William on 01/01/2016.
  */

import java.io.{File, FileInputStream}
import java.util.concurrent.Executors
import javax.inject._

import com.actionfps.gameparser.ProcessJournalApp
import com.actionfps.gameparser.enrichers.JsonGame
import com.actionfps.gameparser.mserver.{ExtractMessage, MultipleServerParser, MultipleServerParserFoundGame}
import akka.agent.Agent
import com.actionfps.accumulation.{GeoIpLookup, ValidServers}
import lib.CallbackTailer
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle
import providers.games.JournalGamesProvider.NewGameCapture

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, blocking}
import ValidServers.Validator._
import ValidServers.ImplicitValidServers._
import org.apache.commons.io.input.Tailer

import scala.util.control.NonFatal

object JournalGamesProvider {

  def getFileGames(file: File) = {
    val fis = new FileInputStream(file)
    try ProcessJournalApp.parseSource(fis)
      .map(_.cg)
      .filter(_.validate.isGood)
      .filter(_.validateServer)
      .map(_.flattenPlayers)
      .map(g => g.id -> g)
      .toMap
    finally fis.close()
  }

  class NewGameCapture(gameAlreadyExists: String => Boolean, afterGame: Option[JsonGame])(registerGame: JsonGame => Unit) {
    var currentState = MultipleServerParser.empty

    def processLine(line: String) = line match {
      case line@ExtractMessage(date, _, _)
        if afterGame.isEmpty || date.isAfter(afterGame.get.endTime.minusMinutes(20)) =>
        currentState = currentState.process(line)
        PartialFunction.condOpt(currentState) {
          case MultipleServerParserFoundGame(fg, _)
            if !gameAlreadyExists(fg.id) && fg.validate.isGood && fg.validateServer =>
            registerGame(fg.flattenPlayers)
        }
      case _ =>
    }
  }

}

/**
  * Load in the list of journals - and tail the last one to grab the games.
  */
@Singleton
class JournalGamesProvider @Inject()(configuration: Configuration,
                                     applicationLifecycle: ApplicationLifecycle)
                                    (implicit executionContext: ExecutionContext)
  extends GamesProvider {

  val logger = Logger(getClass)

  val hooks = Agent(Set.empty[JsonGame => Unit])

  override def addHook(f: (JsonGame) => Unit): Unit = hooks.send(_ + f)

  override def removeHook(f: (JsonGame) => Unit): Unit = hooks.send(_ - f)

  val journalFiles = configuration.underlying.getStringList("af.journal.paths").asScala.map(new File(_))
  val gamesDatas = configuration.underlying.getStringList("af.ladder.games-data").asScala.map(new File(_))

  implicit private val geoIp = GeoIpLookup
  val ex = Executors.newFixedThreadPool(journalFiles.size + 1)
  applicationLifecycle.addStopHook(() => Future(ex.shutdown()))

  val gamesDataF = Future {
    blocking {
      gamesDatas.par.flatMap { file =>
        val src = scala.io.Source.fromFile(file)
        try src.getLines().filter(_.nonEmpty).map { line =>
          try JsonGame.fromJson(line.split("\t")(3))
          catch {
            case NonFatal(e) => logger.error(s"Could not parse JSON line due to ${e}: $line", e)
              throw e
          }
        }.toList.filter(_.validate.isGood)
        finally src.close
      }.map(g => g.id -> g.withGeo.flattenPlayers).toList.toMap
    }
  }

  val gamesA = for {d <- gamesDataF; d <- gamesI(d)} yield d

  private def gamesI(input: Map[String, JsonGame]) = Future {
    blocking {
      val (initialGames, tailIterator) = journalFiles.toList.sortBy(_.lastModified()).reverse match {
        case recent :: rest =>
          val adapter = new IteratorTailerListenerAdapter()
          val tailer = new Tailer(recent, adapter, 2000)
          val reader = GameScanner.tailReader(adapter)
          ex.submit(tailer)
          applicationLifecycle.addStopHook(() => Future(tailer.stop()))
          val (initialRecentGames, tailIterator) = reader.collect(GameScanner.collect)
          val otherGames = rest.par.map { file =>
            val src = scala.io.Source.fromFile(file)
            try src.getLines().scanLeft(GameScanner.zero)(GameScanner.scan).collect(GameScanner.collect).toList
            finally src.close()
          }.toList.flatten
          (otherGames ++ initialRecentGames, tailIterator)
        case Nil =>
          (List.empty, Iterator.empty)
      }
      val jigm = initialGames.filter(_.validate.isGood).map(g => g.id -> g.withGeo).toMap
      val gamesAgent = Agent(jigm ++ input)
      ex.submit(new Runnable {
        override def run(): Unit = {
          tailIterator.foreach { game =>
            if (game.validate.isGood) {
              val gg = game.withGeo.flattenPlayers
              gamesAgent.send(_.updated(gg.id, gg))
              hooks.get().foreach(h => h(gg))
            }
          }
        }
      })
      gamesAgent
    }
  }

  override def games: Future[Map[String, JsonGame]] = gamesA.map(_.get())

}

package providers

import java.io.StringReader
import javax.inject.{Inject, Singleton}

import com.actionfps.accumulation.Clan
import com.actionfps.accumulation.user.User
import com.actionfps.reference._
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.async.Async._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by William on 01/01/2016.
  *
  * Provides reference data from CSV URLs.
  */
@Singleton
class ReferenceProvider @Inject()(configuration: Configuration,
                                  cacheApi: AsyncCacheApi)
                                 (implicit wSClient: WSClient,
                                  executionContext: ExecutionContext) {

  import ReferenceProvider._

  def unCache(): Unit = {
    List(ClansKey, ServersKey, RegistrationsKey, NicknamesKey).foreach(cacheApi.remove)
  }

  private def fetch(key: String) = async {
    await(cacheApi.get[String](key)) match {
      case Some(value) => value
      case None =>
        val value = await(wSClient.url(configuration.underlying.getString(s"af.reference.${key}")).get().filter(_.status == 200).map(_.body))
        await(cacheApi.set(key, value, Duration.apply("1h")))
        value
    }
  }

  object Clans {
    def csv: Future[String] = fetch(ClansKey)

    def clans: Future[List[Clan]] = csv.map { bdy =>
      val sr = new StringReader(bdy)
      try ClanRecord.parseRecords(sr).map(Clan.fromClanRecord)
      finally sr.close()
    }
  }

  object Servers {
    def raw: Future[String] = fetch(ServersKey)

    def servers: Future[List[ServerRecord]] = raw.map { bdy =>
      val sr = new StringReader(bdy)
      try ServerRecord.parseRecords(sr)
      finally sr.close()
    }
  }

  object Users {

    def registrations: Future[List[Registration]] = fetch(RegistrationsKey).map { bdy =>
      val sr = new StringReader(bdy)
      try Registration.parseRecords(sr)
      finally sr.close()
    }

    def rawNicknames: Future[String] = fetch(NicknamesKey)

    def nicknames: Future[List[NicknameRecord]] = rawNicknames.map { bdy =>
      val sr = new StringReader(bdy)
      try NicknameRecord.parseRecords(sr)
      finally sr.close()
    }

    def users: Future[List[User]] = async {
      val regs = await(registrations)
      val nicks = await(nicknames)
      regs.flatMap { reg => User.fromRegistration(reg, nicks) }
    }

  }

  def clans: Future[List[Clan]] = Clans.clans

  def users: Future[List[User]] = Users.users

  private implicit val serverRecordRead = Json.reads[ServerRecord]

  def servers: Future[List[ServerRecord]] = Servers.servers

}

object ReferenceProvider {
  val ClansKey = "clans"
  val ServersKey = "servers"
  val RegistrationsKey = "registrations"
  val NicknamesKey = "nicknames"
}

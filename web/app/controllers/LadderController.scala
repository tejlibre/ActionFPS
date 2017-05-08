package controllers

/**
  * Created by me on 09/05/2016.
  */

import java.time.Instant
import javax.inject._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.actionfps.ladder.parser._
import lib.WebTemplateRender
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import providers.ReferenceProvider
import services.LadderService
import services.LadderService.NickToUser
import views.ladder.Table.PlayerNamer

import scala.concurrent.ExecutionContext
import scala.async.Async._

@Singleton
class LadderController @Inject()(configuration: Configuration,
                                 referenceProvider: ReferenceProvider,
                                 common: WebTemplateRender)
                                (implicit executionContext: ExecutionContext,
                                 actorSystem: ActorSystem) extends Controller {

  private implicit val actorMaterializer = ActorMaterializer()

  private val ladderService = new LadderService(LadderService.getSourceCommands(configuration, "af.ladder.sources"),
    usersMap = () => referenceProvider.users.map(users => new NickToUser{
      override def userOfNickname(nickname: String): Option[String] =
        users.find(_.nickname.nickname == nickname).map(_.id)
    }))

  def aggregate: Aggregate = ladderService.aggregate.displayed(Instant.now()).trimmed(Instant.now())

  def ladder: Action[AnyContent] = Action.async { implicit req =>
    async {
      req.getQueryString("format") match {
        case Some("json") =>
          implicit val aggWriter = {
            implicit val usWriter = Json.writes[UserStatistics]
            Json.writes[Aggregate]
          }
          Ok(Json.toJson(aggregate))
        case _ =>
          implicit val playerNamer = PlayerNamer.fromMap(await(referenceProvider.Users.users).map(u => u.id -> u.name).toMap)
          Ok(common.renderTemplate(
            title = Some("Ladder"),
            supportsJson = true)
          (views.ladder.Table.render(WebTemplateRender.wwwLocation.resolve("ladder_table.html"), aggregate)(showTime = true)))
      }
    }
  }
}

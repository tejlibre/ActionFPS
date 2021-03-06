package controllers

import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors
import javax.inject._

import play.Environment
import play.api.mvc.{Action, AnyContent, Controller}
import scala.concurrent.ExecutionContext

/**
  * Created by William on 01/01/2016.
  *
  * Serve static assets from 'www' directory for development & test purposes.
  */
class Forwarder @Inject()(environment: Environment)(
    implicit executionContext: ExecutionContext)
    extends Controller {

  require(!environment.isProd, s"Environment is ${environment}")

  private val webDistWww = Paths.get("web/dist/www")

  private val distWww = Paths.get("dist/www")

  private def wwwPath = if (Files.exists(webDistWww)) webDistWww else distWww

  private def assetsPath = wwwPath.resolve("assets")

  private def resources = {
    import scala.collection.JavaConverters._
    Files.walk(assetsPath).collect(Collectors.toList[Path]).asScala.toList
  }

  def getAsset(path: String): Action[AnyContent] = {
    resources.find(_.endsWith(path)) match {
      case Some(v) =>
        Action {
          Ok.sendFile(v.toFile)
        }
      case None =>
        Action {
          NotFound("Not found")
        }
    }
  }

}

package controllers

/**
  * Created by William on 05/12/2015.
  */

import javax.inject._

import play.api.Configuration
import play.api.mvc.{Action, Controller}

@Singleton
class Admin @Inject()(configuration: Configuration)
  extends Controller {
  def reloadReference = Action { request =>
    val apiKeyO = request.getQueryString("api-key").orElse(request.headers.get("api-key"))
    val apiKeyCO = configuration.getString("af.admin-api-key")
    if ( apiKeyO == apiKeyCO ) {
      Ok("Done reloading")
    } else {
      Forbidden("Wrong api key.")
    }
  }
}

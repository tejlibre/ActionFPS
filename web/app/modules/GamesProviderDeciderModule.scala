package modules

/**
  * Created by William on 01/01/2016.
  */
import com.actionfps.accumulation.ReferenceMapValidator
import com.actionfps.accumulation.user.GeoIpLookup
import com.actionfps.gameparser.enrichers.{IpLookup, MapValidator}
import play.api.inject._
import play.api.{Configuration, Environment, Logger}
import providers.full.{FullProvider, HazelcastCachedProvider}

import scala.collection.mutable

class GamesProviderDeciderModule extends Module {

  val logger = Logger(getClass)

  override def bindings(environment: Environment,
                        configuration: Configuration): List[Binding[_]] = {
    val bindings = mutable.Buffer.empty[Binding[_]]
    bindings += bind[IpLookup].toInstance(GeoIpLookup)
    val useCached =
      configuration.getString("full.provider").contains("hazelcast-cached")
    logger.info(s"Use cached? ${useCached}")
    if (useCached) {
      bindings += bind[FullProvider].to[HazelcastCachedProvider]
    }
    bindings += bind[MapValidator].toInstance(
      ReferenceMapValidator.referenceMapValidator)
    bindings.toList
  }

}

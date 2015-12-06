package af.rr

import java.io.Reader
import java.net.URI

import org.apache.commons.csv.CSVFormat

import scala.util.Try

case class ClanRecord(id: String, shortName: String, longName: String, website: Option[URI], tag: String, tag2: Option[String])

object ClanRecord {

  def parseRecords(input: Reader): List[ClanRecord] = {
    import collection.JavaConverters._
    CSVFormat.EXCEL.parse(input).asScala.flatMap { rec =>
      for {
        id <- Option(rec.get(0)).filter(_.nonEmpty)
        shortName <- Option(rec.get(1)).filter(_.nonEmpty)
        longName <- Option(rec.get(2)).filter(_.nonEmpty)
        website = Try(new URI(rec.get(3))).toOption
        tag <- Option(rec.get(4)).filter(_.nonEmpty)
        tag2 = Try(rec.get(5)).toOption.filter(_ != null).filter(_.nonEmpty)
      } yield ClanRecord(
        id = id,
        shortName = shortName,
        longName = longName,
        website = website,
        tag = tag,
        tag2 = tag2
      )
    }.toList

  }
}
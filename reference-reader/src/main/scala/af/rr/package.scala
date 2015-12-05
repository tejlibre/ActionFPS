package af

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import scala.util.Try

/**
  * Created by William on 05/12/2015.
  */
package object rr {

  val dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
  val dtf2 = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def parseLocalDateTime(s: String) =
    Try(LocalDateTime.parse(s, dtf)).
      orElse(Try(LocalDate.parse(s, dtf2).atStartOfDay())).
      orElse(Try(LocalDate.parse(s).atStartOfDay())).get

}

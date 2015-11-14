package ac.woop.demo

import java.io.{File, FileInputStream}
import java.util.zip.GZIPInputStream

import akka.util.ByteString
import io.enet.akka.Compressor.CubeQueue

import scala.annotation.tailrec

object UIntApp extends App {
  val headerSize = 428
  lazy val bs = ByteString{
    val is = new FileInputStream(new File("uint"))
    org.apache.commons.io.IOUtils.toByteArray(is)
  }
//  println("First fifty bytes in uint", bs.take(50))
  val q = new CubeQueue(bs)
  val stuffs = Stream.from(0).map {
    num => if ( q.rest.isEmpty ) None else Option(num -> q.getuint)
  }.takeWhile(_.isDefined).flatten
  
  println("Size of stuffs", stuffs.size)
  stuffs.foreach{case (expected, have) =>
    assert(expected == have, s"Expected $expected, got $have")
  }
  
}
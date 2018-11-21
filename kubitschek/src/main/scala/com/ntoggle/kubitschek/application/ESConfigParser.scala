package com.ntoggle.kubitschek.application

import com.google.common.base.Splitter
import com.ntoggle.goldengate.SafeParsers
import com.ntoggle.goldengate.elasticsearch.{ClusterName, ESConfig, ESAddress}
import com.typesafe.config.Config
import scalaz.{Validation, NonEmptyList, Failure}
import scalaz.std.list._
import scalaz.std.string._
import scalaz.syntax.traverse._
import scalaz.syntax.show._
import scalaz.syntax.std.option._
import scala.collection.JavaConverters._

// TODO: Move to ESConfig in goldengate
object ESConfigParser {
  private val addressSplitter = Splitter.on(':').limit(2)
  private def parseAddress(string: String): Option[ESAddress] =
    addressSplitter.split(string).asScala.toList match {
      case hostName :: portString :: Nil =>
        SafeParsers.toInt(portString).map(ESAddress(hostName, _)).toOption
      case other => None
    }

  private def parseAddresses(
    addrs: List[String]): List[ESAddress] =
    addrs.traverseU {
      a => parseAddress(a).toSuccess(s"invalid es address: '$a'").toValidationNel
    }.fold(
      e => throw new Exception(e.shows),
      identity)

  def fromConfig(config: Config): ESConfig = ESConfig(
    ClusterName(config.getString("cluster")),
    parseAddresses(config.getStringList("addresses").asScala.toList),
    Nil)
}
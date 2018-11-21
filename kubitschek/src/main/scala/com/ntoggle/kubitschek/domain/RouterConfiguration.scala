package com.ntoggle.kubitschek
package domain

import java.net.{URI, InetSocketAddress}
import com.ntoggle.kubitschek.api.Port
import play.api.data.validation.ValidationError
import play.api.libs.json.Format

import scalaz.Equal
import scalaz.std.tuple._
import scalaz.syntax.contravariant._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Show, Order}
import scalaz.std.string._


case class ConfigurationEndpoint(host: String, port: Port) {
  def asURI: URI = new URI("http", null, host, port.value, null, null, null)
}
object ConfigurationEndpoint {
  val InvalidHostError = ValidationError("Invalid Host")
  def validHost(implicit r: Reads[String]): Reads[String] =
    Reads.pattern("""(^(([a-zA-Z]|[a-zA-Z][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z]|[A-Za-z][A-Za-z0-9\-]*[A-Za-z0-9])$)|(^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$)""".r,
      InvalidHostError.message).filterNot(InvalidHostError)(_.length > 255)

  implicit val NRouteAddressFormat: Format[ConfigurationEndpoint] = (
    (__ \ "host").format[String](validHost) ~
      (__ \ "port").format[Port]
    )(ConfigurationEndpoint.apply, unlift(ConfigurationEndpoint.unapply))

  implicit val orderConfigurationEndpoint: Order[ConfigurationEndpoint] = Order[(String, Port)]
    .contramap(a => (a.host, a.port))

  implicit val showConfigurationEndpoint: Show[ConfigurationEndpoint] = Show.showFromToString
}

case class RouterConfiguration(
  // the demand partners nRoutes for a supply source (where we configure the rules)
  id: RouterConfigurationId,
  target: ConfigurationEndpoint)
object RouterConfiguration {

  implicit val equalRouterConfiguration: Equal[RouterConfiguration] =
    Equal[(RouterConfigurationId, ConfigurationEndpoint)]
      .contramap(cfg => cfg.id -> cfg.target)

  implicit val showRouterConfiguration: Show[RouterConfiguration] =
    Show[(RouterConfigurationId, ConfigurationEndpoint)]
      .contramap(cfg => cfg.id -> cfg.target)
}

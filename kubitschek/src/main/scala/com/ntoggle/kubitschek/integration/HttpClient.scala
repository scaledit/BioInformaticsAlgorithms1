package com.ntoggle.kubitschek.integration

import java.net.URLEncoder
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

trait GenericServices {
  implicit def system: ActorSystem
  implicit def log: LoggingAdapter
  implicit def ec: ExecutionContext
  implicit def mat: Materializer
}

object HttpClientConfig extends LazyLogging {
  def fromConfig(name: String, config: Config): HttpClientConfig =
    HttpClientConfig(
      name,
      config.getString("host"),
      Try(config.getInt("port")).getOrElse(80),
      Try(config.getBoolean("tls")).toOption.getOrElse(false),
      Try(config.getString("api_key_id")).toOption.filter(_.nonEmpty),
      Try(config.getString("api_secret_id")).toOption.filter(_.nonEmpty))
}
case class HttpClientConfig(
  clientName: String,
  host: String,
  port: Int,
  tls: Boolean,
  username: Option[String],
  password: Option[String]) {
  override def toString: String =
    s"name: '$clientName', host: '$host', port: '$port', tls: '$tls', username: '$username', password: '${password.map(_ => "********")}'"
}

object HttpClient {

  def encode(value: String): String = URLEncoder.encode(value, "UTF-8")

  def queryString(queryParams: Map[String, String]): String =
    if (queryParams.nonEmpty)
      "?" + queryParams
        .filterNot {
        case (key, value) ⇒ key.length == 0
      }.mapValues(encode)
        .toList
        .map {
        case (key, value) ⇒ s"$key=$value"
      }.mkString("&")
    else ""

  def header(key: String, value: String): Option[HttpHeader] =
    HttpHeader.parse(key, value) match {
      case ParsingResult.Ok(header, errors) ⇒ Option(header)
      case _                                ⇒ None
    }

  def headers(headersMap: Map[String, String]): List[HttpHeader] =
    headersMap.flatMap {
      case (key, value) ⇒ header(key, value)
    }.toList

  def apply(
    host: String,
    port: Int,
    tls: Boolean)(implicit system: ActorSystem, mat: Materializer): HttpClient =
    new HttpClientImpl("generic", host, port, tls)

  def fromConfig(
    config: HttpClientConfig)(
    implicit system: ActorSystem, mat: Materializer): HttpClient =
    new HttpClientImpl(
      config.clientName,
      config.host,
      config.port,
      config.tls,
      config.username,
      config.password)
}

trait HttpClient extends GenericServices {

  def clientName: String
  def host: String
  def port: Int
  def tls: Boolean
  def username: Option[String]
  def password: Option[String]

  private def tlsConnection(host: String, port: Int) =
    Http().outgoingConnectionTls(host, port)

  private def httpConnection(host: String, port: Int) =
    Http().outgoingConnection(host, port)

  private def connection =
    if (tls) tlsConnection(host, port) else httpConnection(host, port)

  private def requestPipeline(request: HttpRequest): Future[HttpResponse] =
    Source.single(request)
      .map(addCredentials)
      .log(s"$clientName-request")
      .via(connection)
      .log(s"$clientName-response")
      .runWith(Sink.head)

  private def addCredentials(request: HttpRequest): HttpRequest =
    RequestBuilding.addCredentials(
      BasicHttpCredentials(
        username.getOrElse(""),
        password.getOrElse("")))(request)

  import scala.collection.JavaConversions._
  def get(url: String, queryParamsMap: Map[String, String] = Map.empty, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    requestPipeline(RequestBuilding.Get(url + HttpClient.queryString(queryParamsMap)).addHeaders(HttpClient.headers(headersMap)))

  // figure out how to pass in a HttpEntity - there was a problem with an implicit I couldn't find
  def post(url: String, content: String, headersMap: Map[String, String] = Map.empty): Future[HttpResponse] =
    requestPipeline(RequestBuilding.Post(url,
      HttpEntity(
        ContentType(MediaTypes.`application/x-www-form-urlencoded`), content)).addHeaders(HttpClient.headers(headersMap)))

}

class HttpClientImpl(
  val clientName: String,
  val host: String,
  val port: Int,
  val tls: Boolean,
  val username: Option[String] = None,
  val password: Option[String] = None)(implicit val system: ActorSystem, val mat: Materializer)
  extends HttpClient {
  implicit lazy val log: LoggingAdapter = Logging(system, s"${this.getClass.getName}.$clientName")
  implicit lazy val ec: ExecutionContext = system.dispatcher
}
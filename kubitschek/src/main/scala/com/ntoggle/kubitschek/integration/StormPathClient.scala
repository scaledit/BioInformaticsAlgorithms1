package com.ntoggle.kubitschek
package integration

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.ntoggle.albi.SupplyPartner
import com.typesafe.scalalogging.slf4j.LazyLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import com.typesafe.config.Config
import play.api.libs.json.JsValue
import scala.concurrent.{Future, ExecutionContext}
import scalaz.{-\/, \/-, \/}

object StormPathClientConfig {
  def fromConfig(config: Config): StormPathClientConfig =
    StormPathClientConfig(
      config.getString("app_id"),
      HttpClientConfig.fromConfig(
        "StormPath",
        config))
}
case class StormPathClientConfig(
  appId: String,
  httpClientConfig: HttpClientConfig)


trait StormPathApi {
  def generateToken(username: Username, password: Password): Future[AuthenticationError \/ StormPathOAuthToken]
  def refreshToken(token: AccessToken): Future[AuthenticationError \/ StormPathOAuthToken]
  def authToken(token: AccessToken): Future[UnauthorizedAccess \/ AuthenticatedUser]
}

object StormPathClient {
  def fromConfig(config: StormPathClientConfig)(implicit system: ActorSystem, mat: Materializer): StormPathApi =
    new StormPathClient(
      config.appId,
      HttpClient.fromConfig(config.httpClientConfig))
}
class StormPathClient(
  appId: String,
  httpClient: HttpClient)(implicit val system: ActorSystem, val mat: Materializer)
  extends StormPathApi
  with LazyLogging {

  implicit val ec: ExecutionContext = system.dispatcher

  val basePath = s"/v1/applications/$appId"

  def generateToken(username: Username, password: Password): Future[AuthenticationError \/ StormPathOAuthToken] = {
    val response = httpClient.post(
      s"$basePath/oauth/token",
      s"grant_type=password&username=${username.value}&password=${password.value}")
    .flatMap[AuthenticationError \/ StormPathOAuthToken] { res =>
      res.status match {
        case StatusCodes.OK => Unmarshal(res.entity).to[StormPathOAuthToken].map(\/-(_))
        case _ => Unmarshal(res.entity).to[AuthenticationError].map(-\/(_))
      }
    }

    response.onFailure {
      case e => logger.warn(s"Unable to get token '$e'")
    }
    response
  }

  def refreshToken(token: AccessToken): Future[AuthenticationError \/ StormPathOAuthToken] = {
    httpClient.post(
      s"$basePath/oauth/token",
      s"grant_type=refresh_token&refresh_token=${token.value}")
    .flatMap[AuthenticationError \/ StormPathOAuthToken] { res =>
      res.status match {
        case StatusCodes.OK => Unmarshal(res.entity).to[StormPathOAuthToken].map(\/-(_))
        case _ => Unmarshal(res.entity).to[AuthenticationError].map(-\/(_))
      }
    }
  }

  /**
   * Don't like the fact that 3 network calls are made here; not sure there's
   * a way around this ATM, until we do more client-side caching (which has it's own issues)
   */
  def authToken(token: AccessToken): Future[UnauthorizedAccess \/ AuthenticatedUser] = {
    httpClient.get(s"$basePath/authTokens/${token.value}")
      .flatMap[UnauthorizedAccess \/ AuthenticatedUser] { res =>
        res.status match {
          case StatusCodes.Found =>
            val auth = for {
              token <- httpClient.get(res.header[Location].get.getUri().toString).flatMap {
                resp => Unmarshal(resp.entity).to[JsValue] }
              accountUrl = (token \ "account" \ "href").as[String] + "?expand=customData"
              account <- httpClient.get(accountUrl).flatMap {
                resp => Unmarshal(resp.entity).to[JsValue] }
            } yield account

            auth.map { a =>
              \/-(AuthenticatedUser(
                (a \ "username").as[Username],
                (a \ "email").as[EmailAddress],
                (a \ "fullName").as[Fullname],
                UserPreferences.Default,
                (a \ "customData" \ "organization").as[Organization],
                DemandConfiguration((a \ "customData" \ "supplyPartners").as[List[SupplyPartner]])))
            }
          case _ => Future(-\/(UnauthorizedAccess(token)))
      }
    }
  }

}


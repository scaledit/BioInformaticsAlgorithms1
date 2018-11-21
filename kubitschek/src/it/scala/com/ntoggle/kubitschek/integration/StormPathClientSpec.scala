package com.ntoggle.kubitschek
package integration

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.ntoggle.albi._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.specs2.mutable.Specification
import org.specs2.scalaz.ScalazMatchers

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scalaz.syntax.either._


class StormPathClientSpec
  extends Specification
  with ScalazMatchers
  with LazyLogging {
  sequential

  private val config = {
    val cfg = ConfigFactory.load("stormpath")
    StormPathClientConfig.fromConfig(cfg.getConfig("storm-path"))
  }
  private val httpServiceHost = "localhost"
  private val httpServicePort = 2000
  private val baseHost = s"http://$httpServiceHost:$httpServicePort"
  private val tokenValue = "token-id"
  private val refreshValue = "refresh-id"

  val newId = () => UUID.randomUUID().toString
  val dpId = DemandPartnerId("2e83b2ea-0e9e-4d4d-a330-a1d0b9065270")


  "generateToken for valid credentials" ! {

    val token = StormPathOAuthToken(
      AccessToken(tokenValue),
      AccessToken(refreshValue),
      TokenType("Bearer"),
      TokenExpiresInSec(3600),
      s"$baseHost/v1/accessTokens/r0klomitodnOCuvESIP5"
    )

    withServer {
      withClient(config) { client =>
        for {
          t <- client.generateToken(Username("test"), Password("test"))
        }
          yield t must equal(token.right)
      }
    }
  }

  "generateToken fails for invalid valid credentials" ! {

    val error = AuthenticationError(
      200,
      1234,
      "error message",
      "developer error message",
      "more info",
      "the real error")

    withServer {
      withClient(config) { client =>
        for {
          t <- client.generateToken(Username("test-bad"), Password("test-bad"))
        }
          yield t must equal(error.left)
      }
    }
  }

  "refreshToken" ! {

    val accessToken = AccessToken(refreshValue)

    val token = StormPathOAuthToken(
      AccessToken(tokenValue),
      AccessToken(refreshValue),
      TokenType("Bearer"),
      TokenExpiresInSec(3600),
      s"$baseHost/v1/accessTokens/r0klomitodnOCuvESIP5"
    )

    withServer {
      withClient(config) { client =>
        for {
          t <- client.refreshToken(accessToken)
        }
          yield t must equal(token.right)
      }
    }
  }

  "refreshToken fails for invalid token" ! {

    val accessToken = AccessToken("not-correct")

    val error = AuthenticationError(
      200,
      1234,
      "error message",
      "developer error message",
      "more info",
      "the real error")

    withServer {
      withClient(config) { client =>
        for {
          t <- client.refreshToken(accessToken)
        }
          yield t must equal(error.left)
      }
    }
  }

  "authToken correct" ! {

    val accessToken = AccessToken(tokenValue)

    val user =
      AuthenticatedUser(
        Username("test"),
        EmailAddress("test@ntoggle.com"),
        Fullname("Test Test"),
        UserPreferences.Default,
        Organization(DemandPartnerOrganizationId(dpId), "name"),
        DemandConfiguration(List(SupplyPartner(SupplyPartnerId("9a726b7f-e36a-441f-b83a-2581c0edfcd3"), SupplyPartnerName("EXCH1"))))
      )

    withServer {
      withClient(config) { client =>
        for {
          t <- client.authToken(accessToken)
        } yield t must equal(user.right)
      }
    }
  }

  "authToken fails for wrong token" ! {

    val accessToken = AccessToken("bad-token")
    val error = UnauthorizedAccess(accessToken)

    withServer {
      withClient(config) { client =>
        for {
          t <- client.authToken(accessToken)
        } yield t must equal(error.left)
      }
    }
  }


  def withClient[A](config: StormPathClientConfig)(f: StormPathApi => Future[A]): () => Future[A] = {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val client: StormPathApi = StormPathClient.fromConfig(config)
    () => f(client)
  }


  def withServer[A](f: () => Future[A]): A = {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val httpService = new MockStormPathService(
      baseHost,
      (Username("test"), Password("test")),
      config.appId,
      "account-id", AccessToken(tokenValue), AccessToken(refreshValue)
    )

    try {
      val binding = Http().bindAndHandle(
        httpService.routes,
        httpServiceHost,
        httpServicePort)
      Await.result(f(), 2000.milli)
    } finally {
      system.shutdown()
    }
  }

}
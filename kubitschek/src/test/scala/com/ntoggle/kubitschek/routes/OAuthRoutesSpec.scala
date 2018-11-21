package com.ntoggle.kubitschek.routes

import akka.http.scaladsl.model.{FormData, HttpHeader}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Rejection
import akka.http.scaladsl.testkit.RouteTest
import akka.http.specs2.Specs2Interface
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.infra._
import com.ntoggle.kubitschek.integration._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.JsString

import scala.concurrent.Future
import scalaz.std.scalaFuture._
import scalaz.{\/-, -\/, EitherT, \/}

class OAuthRoutesSpec
  extends Specification
  with RouteTest
  with Specs2Interface
  with ScalaCheck {

  import PlayJsonSupportExt._

  sequential

  def oAuthRoutes(
    getToken: (Username, Password) => ApiResponseFuture[OAuthToken] =
    (_, _) => EitherT.right(Future.failed[OAuthToken](new Exception("get token should not have been called"))),
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    _ => Future.failed(new Exception("check auth should not have been called"))
    ): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          OAuthRoutes.route(
            getToken,
            checkAuthentication)
        }
      }
    }

  "OAuth Routes" should {

    "Create token and return it" in {

      val token = AccessToken("token")
      val refresh = AccessToken("refresh")
      val tokenType = TokenType("type")
      val expiresIn = TokenExpiresInSec(3600)

      val route = oAuthRoutes(
        getToken = (_name, _password) =>
          EitherT.right[Future, Rejection, OAuthToken] {
            Future.successful(
              OAuthToken(token, refresh, tokenType, expiresIn))
          })

      Post("/token", FormData("username" -> "test", "password" -> "testpw", "grant_type" -> "password")) ~> route ~> check {
        responseAs[OAuthToken].accessToken ==== token
        responseAs[OAuthToken].refreshToken ==== refresh
        responseAs[OAuthToken].tokenType ==== tokenType
        responseAs[OAuthToken].expiresInSec ==== expiresIn
      }

    }


    "Reject getting token without form fields" in {
      Post("/token") ~> oAuthRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing form parameter."
        status ==== NotFound
      }
    }

    "Reject getting token without username" in {
      Post("/token", FormData("password" -> "test", "grant_type" -> "password")) ~> oAuthRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing form parameter."
        error.cause ==== Some(JsString("username"))
        status ==== NotFound
      }
    }

    "Reject getting token without password" in {
      Post("/token", FormData("username" -> "test", "grant_type" -> "password")) ~> oAuthRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing form parameter."
        error.cause ==== Some(JsString("password"))
        status ==== NotFound
      }
    }

    "Reject getting token without grant_type" in {
      Post("/token", FormData("username" -> "test", "password" -> "test")) ~> oAuthRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing form parameter."
        error.cause ==== Some(JsString("grant_type"))
        status ==== NotFound
      }
    }

    "Reject getting token without grant_type == password" in {
      Post("/token", FormData("username" -> "test", "password" -> "testpw", "grant_type" -> "nothing")) ~> oAuthRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message.contains("supported grant_types are: 'password'")
        error.cause ==== None
        status ==== BadRequest
      }
    }

  }
}


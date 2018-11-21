package com.ntoggle.kubitschek.routes

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.ParameterDirectives
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.infra.{SecurityDirectives, BadRequestRejection, PlayJsonSupportExt}
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.routes.SupplyPartnerRoutes._

import scala.concurrent.Future
import scalaz.\/


object OAuthRoutes
  extends ParameterDirectives
  with SecurityDirectives {

  import PlayJsonSupportExt._

  def route(
    getToken: (Username, Password) => ApiResponseFuture[OAuthToken],
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser]
    )(implicit materializer: Materializer): Route =
    pathPrefix("token") {
      post {
        formFields('username.as[String], 'password.as[String], 'grant_type.as[String])
        { (username, password, grantType) =>
          grantType match {
            case "password" =>
              onApiResponseFutureComplete(
                getToken(Username(username), Password(password)))(complete(_))
            case _ =>
              reject(BadRequestRejection(s"bad grant_type '${grantType}'. Supported grant_types are: 'password'", None))
          }
        }
      }
    }
}

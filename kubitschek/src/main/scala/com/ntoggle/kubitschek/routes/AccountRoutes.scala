package com.ntoggle.kubitschek.routes

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.ParameterDirectives
import akka.stream.Materializer
import com.ntoggle.kubitschek.infra.{PlayJsonSupportExt, SecurityDirectives}
import com.ntoggle.kubitschek.integration._

import scala.concurrent.Future
import scalaz.\/


object AccountRoutes
  extends ParameterDirectives
  with SecurityDirectives {

  import PlayJsonSupportExt._

  def route(
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser],
    userDetails: AuthenticatedUser => UserDetails,
    demandConfig: AuthenticatedUser => DemandConfiguration
    )(implicit materializer: Materializer): Route =

    pathPrefix("user") {

  (get & pathPrefix("configuration" / "demand-partner")) {
      authenticate(checkAuthentication) { user =>
        complete(demandConfig(user))
      }
    } ~
    (get & pathEnd) {
      authenticate(checkAuthentication) { user =>
        complete(userDetails(user))
      }
    }
}
}

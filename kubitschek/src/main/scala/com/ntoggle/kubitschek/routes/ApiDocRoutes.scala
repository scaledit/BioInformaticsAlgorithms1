package com.ntoggle.kubitschek
package routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

case class ApiDocConfig(publicRoot: String)

object ApiDocRoutes {

  def route(config: ApiDocConfig) : Route = pathPrefix("docs") {

    pathSingleSlash {
      getFromResource(s"${config.publicRoot}/apidoc/viewer/index.html")
    } ~
    path ("api-client.json") {
      getFromResource(s"${config.publicRoot}/apidoc/api-client.json")
    } ~
    path("license.html") {
      getFromResource(s"${config.publicRoot}/apidoc/license.html")
    } ~ {
      getFromResourceDirectory(s"${config.publicRoot}/apidoc/viewer/")
    }
  }

}

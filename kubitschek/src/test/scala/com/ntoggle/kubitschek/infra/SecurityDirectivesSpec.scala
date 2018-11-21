package com.ntoggle.kubitschek.infra

import java.util.UUID

import akka.http.scaladsl.model.{HttpHeader, Uri}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.RouteTest
import akka.http.specs2.Specs2Interface
import com.ntoggle.albi.DemandPartnerId
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.integration._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scalaz.{\/-, -\/, \/}


class SecurityDirectivesSpec extends Specification with RouteTest with Specs2Interface with ScalaCheck with SecurityDirectives {
  import PlayJsonSupportExt._
  import com.ntoggle.kubitschek.infra.ParamExtractorDirectives._

  val newId = () => UUID.randomUUID().toString

  def testRoutes(
    test: Route,
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser]): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          authenticate(checkAuthentication) { auth =>
            pathPrefix("test") {
              complete(auth)
            }
          }
        }
      }
    }

  "SecurityDirectives" should {

    "reject" in {

      val route: Route = (get & queryParamMap) {
        complete(_)
      }

      val checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
        (_) => Future.successful[UnauthorizedAccess \/ AuthenticatedUser](-\/(UnauthorizedAccess(AccessToken(""))))

      Get(Uri("/test")) ~> testRoutes(route, checkAuthentication) ~> check {
        responseAs[Map[String, String]] ==== Map(
          "message" -> "The user is not authenticated to access this resource",
          "cause" -> "Invalid token ''")
      }
    }

    "allow" in {

      val user = AuthenticatedUser(
        Username("username"),
        EmailAddress("email@me.com"),
        Fullname("fullname"),
        UserPreferences.Default,
        Organization(DemandPartnerOrganizationId(DemandPartnerId(newId())),""),
        DemandConfiguration(List.empty)
      )

      val route: Route = (get & queryParamMap) {
        complete(_)
      }

      val checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
        (_) => Future.successful[UnauthorizedAccess \/ AuthenticatedUser](\/-(user))

      Get(Uri("/test")) ~> testRoutes(route, checkAuthentication) ~> check {
        responseAs[AuthenticatedUser] ==== user
      }
    }
  }
}

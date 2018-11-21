package com.ntoggle.kubitschek.routes

import java.util.UUID

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.RouteTest
import akka.http.specs2.Specs2Interface
import com.ntoggle.albi.{DemandPartnerId, SupplyPartnerName, SupplyPartnerId, SupplyPartner}
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.infra.{ErrorMessage, PlayJsonSupportExt}
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.services.AuthenticationService
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scalaz.{\/-, -\/, \/}

class AccountRoutesSpec
  extends Specification
  with RouteTest
  with Specs2Interface
  with ScalaCheck {

  import PlayJsonSupportExt._

  sequential

  val newId = () => UUID.randomUUID().toString

  def accountRoutes(
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    _ => Future.failed(new Exception("check auth should not have been called")),
    userDetails: AuthenticatedUser => UserDetails = u => AuthenticationService.userDetails(u),
    demandConfig: AuthenticatedUser => DemandConfiguration = u => AuthenticationService.demandConfig(u)
    ): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          AccountRoutes.route(
            checkAuthentication,
            userDetails,
            demandConfig
          )
        }
      }
    }

  "Account Routes" should {
    "not return user information if not authenticated" in {

      val route = accountRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            -\/(UnauthorizedAccess(AccessToken("123"))))
      )

      Get("/user") ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "The user is not authenticated to access this resource"
        status ==== Unauthorized
      }
    }

    "return user information" in {
      val org = Organization(DemandPartnerOrganizationId(DemandPartnerId(newId())),"")
      val userDetails = UserDetails(
        Username("bob"),
        EmailAddress("12345@test.com"),
        Fullname("bob here"),
        UserPreferences.Default,
        org
      )

      val user = AuthenticatedUser(
        userDetails.username,
        userDetails.email,
        userDetails.fullname,
        userDetails.preferences,
        userDetails.organization,
        DemandConfiguration(List.empty)
      )

      val route = accountRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)))

      Get("/user") ~> route ~> check {
        responseAs[UserDetails] ==== userDetails
      }
    }

    "return supply information" in {
      val org = Organization(DemandPartnerOrganizationId(DemandPartnerId(newId())),"")
      val dc = DemandConfiguration(List(SupplyPartner(SupplyPartnerId("9a726b7f-e36a-441f-b83a-2581c0edfcd3"),SupplyPartnerName("sp1"))))
      val userDetails = UserDetails(
        Username("bob"),
        EmailAddress("12345@test.com"),
        Fullname("bob here"),
        UserPreferences.Default,
        org
      )

      val user = AuthenticatedUser(
        userDetails.username,
        userDetails.email,
        userDetails.fullname,
        userDetails.preferences,
        userDetails.organization,
        dc
      )

      val route = accountRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)))

      Get("/user/configuration/demand-partner") ~> route ~> check {
        responseAs[DemandConfiguration] ==== dc
      }
    }
  }
}

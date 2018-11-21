package com.ntoggle.kubitschek.service

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import com.ntoggle.albi.DemandPartnerId
import com.ntoggle.kubitschek.infra.{AuthenticationRejection}
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.services.AuthenticationService
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz._

class AuthenticationServiceSpec extends Specification {

  "AuthenticationService" should {

    "authenticate() returns a valid token" ! {

      val token = StormPathOAuthToken(
        AccessToken("token"),
        AccessToken("refresh"),
        TokenType("type"),
        TokenExpiresInSec(3600),
        "tokenUrl"
      )

      def authorize = AuthenticationService.authenticate(
        (_, _) => Future.successful[AuthenticationError \/ StormPathOAuthToken](\/-(token)))

      val result = Await.result(authorize(Username("me"), Password("pw")).run, 1000.milli)
      result ==== \/-(token.toOAuthToken)
    }

    "authenticate() fails when wrong credentials" ! {

      val error = AuthenticationError(1,1,"msg", "dev-msg","more","error")

      def authenticate = AuthenticationService.authenticate(
        (_, _) => Future.successful[AuthenticationError \/ StormPathOAuthToken](-\/(error)))

      val result = Await.result(authenticate(Username("me"), Password("pw")).run, 1000.milli)
      result ==== -\/(AuthenticationRejection(error.message))
    }

    "checkAuthentication() returns a valid user" ! {

      val token = AccessToken("token")
      val user = AuthenticatedUser(
        Username("username"),
        EmailAddress("email@me.com"),
        Fullname("fullname"),
        UserPreferences.Default,
        Organization(DemandPartnerOrganizationId(DemandPartnerId("")),""),
        DemandConfiguration(List.empty)
      )
      val headers = Seq[HttpHeader](RawHeader("Authorization", "Bearer c3c3c3c3c"))

      def authenticate = AuthenticationService.checkAuthentication(
        (_) => Future.successful[UnauthorizedAccess \/ AuthenticatedUser](\/-(user)))

      val result = Await.result(authenticate(headers), 1000.milli)
      result ==== \/-(user)
    }

    "checkAuthentication() fails when no credentials" ! {

      def authenticate = AuthenticationService.checkAuthentication(
        (_) => Future.failed(new Exception("check auth should not have been called")))

      val result = Await.result(authenticate(Seq.empty[HttpHeader]), 1000.milli)
      result ==== -\/(UnauthorizedAccess(AccessToken("")))
    }

    "checkAuthentication() fails when wrong credentials" ! {

      def error = UnauthorizedAccess(AccessToken("c3c3c3c3c"))
      val headers = Seq[HttpHeader](RawHeader("Authorization", "Bearer c3c3c3c3c"))

      def authenticate = AuthenticationService.checkAuthentication(
        (_) => Future.successful[UnauthorizedAccess \/ AuthenticatedUser](-\/(error)))

      val result = Await.result(authenticate(headers), 1000.milli)
      result ==== -\/(error)
    }


  }
}


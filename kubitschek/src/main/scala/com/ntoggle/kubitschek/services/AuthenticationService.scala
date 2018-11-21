package com.ntoggle.kubitschek.services

import akka.http.scaladsl.model.HttpHeader
import com.ntoggle.albi.{SupplyPartner, DemandPartnerId}
import com.ntoggle.kubitschek.api.ApiResponseFuture
import com.ntoggle.kubitschek.integration._
import scala.concurrent.{Future, ExecutionContext}
import scalaz.{-\/, \/, EitherT}
import ExecutionContext.Implicits.global
import scalaz.std.scalaFuture._


// sample credentials: ("test@ntoggle.com", "T3stM3Now")
object AuthenticationService {

  def authenticate(
    getToken: (Username, Password) => Future[AuthenticationError \/ StormPathOAuthToken]
    ): (Username, Password) => ApiResponseFuture[OAuthToken] =
    (username, password) =>
    {
      EitherT.eitherT[Future, AuthenticationError,OAuthToken](getToken(username, password).map { _.map( _.toOAuthToken)})
        .leftMap(OAuthRejections.rejection)
    }

  def checkAuthentication(
    check: (AccessToken) => Future[UnauthorizedAccess \/ AuthenticatedUser]
    ): Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    (headers) => {
      val header = headers.find(_.name() == "Authorization")
      header match {
        case None => Future(-\/(UnauthorizedAccess(AccessToken(""))))
        case Some(h) => check(AccessToken(h.value().substring("Bearer".length + 1)))
      }
    }

  def userDetails(user: AuthenticatedUser): UserDetails = {

    UserDetails(
    user.username,
    user.email,
    user.fullname,
    user.preferences,
    user.organization
    )
  }

  def demandConfig(u: AuthenticatedUser): DemandConfiguration = {
    u.demandConfig
  }

  def demandPartner(u: AuthenticatedUser) : DemandPartnerId = {
    u.organization.id match {
      case DemandPartnerOrganizationId(s) => s
      case _ => DemandPartnerId("notfound")
    }
  }

  def supplyPartners(u: AuthenticatedUser) : List[SupplyPartner] = {
    u.demandConfig.supplyPartners
  }


}

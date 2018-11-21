package com.ntoggle.kubitschek.infra

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.unmarshalling.{Unmarshaller, FromRequestUnmarshaller}
import com.ntoggle.albi.{SupplyPartner, DemandPartnerId, SupplyPartnerId}
import com.ntoggle.kubitschek.integration.{DemandPartnerOrganizationId, AuthenticatedUser, UnauthorizedAccess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-, \/}

trait SecurityDirectives extends AnyRef {

  def authenticate(check: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser]): Directive[Tuple1[AuthenticatedUser]] =
    Directive[Tuple1[AuthenticatedUser]] {
      inner => ctx =>
        check(ctx.request.headers).flatMap {
          case \/-(x) => inner(Tuple1(x))(ctx)
          case -\/(x) => ctx.reject(AuthenticationRejection(s"Invalid token '${x.token.value}'"))
        }
    }

  def authorizeDp(dpId: DemandPartnerId, user: AuthenticatedUser): Directive[Tuple1[(DemandPartnerId)]] =
    Directive[Tuple1[(DemandPartnerId)]] {
      inner => ctx =>
      {
        if ( user.demandPartner == dpId )
          inner(Tuple1(user.demandPartner))(ctx)
        else
          ctx.reject(AuthorizationRejection(s"Invalid dpId '${dpId.id}'"))
      }
    }

  def authorizeSp(spId: SupplyPartnerId, user: AuthenticatedUser): Directive[(DemandPartnerId,SupplyPartnerId)] =
    Directive[(DemandPartnerId, SupplyPartnerId)] {
      inner => ctx =>
       {
       if ( user.demandConfig.supplyPartners.map(_.id).contains(spId) )
        inner(user.demandPartner, spId)(ctx)
       else
       ctx.reject(AuthorizationRejection(s"Invalid spId '${spId.id}'"))
        }
    }

  /*
  This Directive is used for cases where spId is optional in the request.
  if no spId, we allow the route to run, and assume you will filter later on valid spId values.
  if spId is provided in the request, validate that user is authorized to access it.
   */
  def authorizeOptionalSp(spId: Option[SupplyPartnerId], user: AuthenticatedUser): Directive[(DemandPartnerId, Option[SupplyPartnerId])] =
    Directive[(DemandPartnerId,Option[SupplyPartnerId])] {
      inner => ctx => {
        spId match {
        case Some(s) =>
          if ( user.supplyPartners.map(_.id).contains(s) )
                  inner(user.demandPartner, spId)(ctx)
                 else
                 ctx.reject(AuthorizationRejection(s"Invalid spId '${s.id}'"))

          case None => inner(user.demandPartner, spId)(ctx)

        }
     }
   }
}


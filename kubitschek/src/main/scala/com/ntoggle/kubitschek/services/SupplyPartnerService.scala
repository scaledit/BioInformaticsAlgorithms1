package com.ntoggle.kubitschek
package services

import com.ntoggle.albi.{SupplyPartner, SupplyPartnerId, SupplyPartnerName}
import com.ntoggle.kubitschek.api.ApiResponseFuture
import com.ntoggle.kubitschek.domain._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.scalaFuture._
import scalaz.{EitherT, \/}

object SupplyPartnerService {
  def create(
    newId: () => Future[String],
    addSupplyPartner: SupplyPartner => Future[ConfigurationError \/ SupplyPartner])
    (implicit ctx: ExecutionContext):
    (SupplyPartnerName) => ApiResponseFuture[SupplyPartner] =

    (name) =>
      for {
        id <- EitherT.right(newId()).map(SupplyPartnerId.apply)
        sp = SupplyPartner(
          id,
          name)
        result <- EitherT.eitherT(addSupplyPartner(sp))
          .leftMap(ConfigurationErrorRejections.rejection)
      } yield result

  def get(getSupplyPartner: SupplyPartnerId => Future[Option[SupplyPartner]]):
    SupplyPartnerId => Future[Option[SupplyPartner]] = getSupplyPartner

  def list(
    listSupplyPartners: (Offset, Limit) => Future[List[SupplyPartner]])(
    implicit ctx: ExecutionContext):
  (Offset, Limit) => Future[List[SupplyPartner]] =
    (o: Offset, l: Limit) => listSupplyPartners(o, l).map(_.toList)

}
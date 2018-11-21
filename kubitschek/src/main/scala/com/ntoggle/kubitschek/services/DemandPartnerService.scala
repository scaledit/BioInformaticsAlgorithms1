package com.ntoggle.kubitschek
package services

import com.ntoggle.albi.{DemandPartner, DemandPartnerId, DemandPartnerName}
import com.ntoggle.kubitschek.api.ApiResponseFuture
import com.ntoggle.kubitschek.domain.{Limit, Offset, ConfigurationError}

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{EitherT, \/}
import scalaz.std.scalaFuture._

object DemandPartnerService {

  def create(
    newId: () => Future[String],
    addDemandPartner: DemandPartner => Future[ConfigurationError \/ DemandPartner])
    (implicit ctx: ExecutionContext):
    (DemandPartnerName) => ApiResponseFuture[DemandPartner] = {

    name =>

      for {
        id <- EitherT.right(newId()).map(DemandPartnerId.apply)
        dp = DemandPartner(id, name)
        result <- EitherT.eitherT(addDemandPartner(dp))
          .leftMap(ConfigurationErrorRejections.rejection)
      } yield result
  }

  def get(getDemandPartner: DemandPartnerId => Future[Option[DemandPartner]]):
    DemandPartnerId => Future[Option[DemandPartner]] = getDemandPartner

  def list(listDemandPartners: (Offset, Limit) => Future[List[DemandPartner]]):
    (Offset, Limit) => Future[List[DemandPartner]] = listDemandPartners
}
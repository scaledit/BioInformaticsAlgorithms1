package com.ntoggle.kubitschek
package services

import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId, EstimatedAvailableQps}
import com.ntoggle.kubitschek.api.{GetForecastResponse, ApiResponseFuture}
import com.ntoggle.kubitschek.domain.{ConfigurationError, RuleConditions}
import scala.concurrent.{Future, ExecutionContext}
import scalaz.{EitherT, \/}

import scalaz.std.scalaFuture._

object ForecastService {
  def get(
    getEstimatedQPS: (SupplyPartnerId, DemandPartnerId, TrafficType, RuleConditions) => Future[EstimatedAvailableQps]
    )(implicit ctx: ExecutionContext):
  (SupplyPartnerId, DemandPartnerId, TrafficType, RuleConditions) => ApiResponseFuture[GetForecastResponse] =
    (spId, dpId, dt, rc) => for {
      r <- EitherT.right(getEstimatedQPS(spId, dpId, dt, rc))
    } yield GetForecastResponse(r)
}

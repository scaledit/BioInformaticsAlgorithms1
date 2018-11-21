package com.ntoggle.kubitschek.estimation

import com.ntoggle.albi.{SupplyPartnerId, DemandPartnerId, EstimatedAvailableQps}
import com.ntoggle.kubitschek.domain._

import scala.concurrent.Future

object EstimateStub {

  val estimateRule: (SupplyPartnerId, DemandPartnerId, RuleConditions) =>
    Future[EstimatedAvailableQps] =
    (_, _, _) => Future.successful(EstimatedAvailableQps(10000))


  val estimateFromRuleId: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast] =
    (_, _, _) => Future.successful(RuleAvailableForecast(Some(EstimatedAvailableQps(33333))))

  val estimateEndpointAvailable: RouterConfigurationId => Future[EndpointAvailableForecast] =
    _ => Future.successful(EndpointAvailableForecast(Some(EstimatedAvailableQps(245000))))
}

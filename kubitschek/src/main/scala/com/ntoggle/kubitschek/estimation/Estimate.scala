package com.ntoggle.kubitschek
package estimation

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.humber.{estimation => humber}
import com.ntoggle.humber.estimation.EstimateConfig
import com.ntoggle.goldengate.scalazint.Syntax.RichTask
import com.ntoggle.kubitschek.domain._
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{OptionT, ReaderT}
import scalaz.std.scalaFuture._
import scalaz.concurrent.Task

object Estimate extends LazyLogging {

  def estimateFromRuleId(
    estimateConfig: EstimateConfig,
    getRule: RuleId => Future[Option[Rule]])(implicit ctx: ExecutionContext):
  (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast] =
    (spId, dpId, ruleId) => (for {
      r <- OptionT.optionT(getRule(ruleId))
      _ <- OptionT.optionT(
        Future.successful {
          logger.trace(s"Attempting to estimate with $r")
          Option(())
        })
      e <- OptionT.optionT(estimate(estimateConfig)(spId, dpId, r.trafficType, r.conditions).map(Option.apply))
    } yield e).run.map(RuleAvailableForecast.apply)

  private def estimateTask(
    spId: SupplyPartnerId,
    dpId: DemandPartnerId,
    trafficType: TrafficType,
    conditions: RuleConditions): ReaderT[Task, EstimateConfig, EstimatedAvailableQps] =
    humber.Estimate.estimateRule(
      spId,
      dpId,
      Mapping.toHumberRuleConditions(trafficType, conditions))

  def estimate(
    estimateConfig: EstimateConfig): (SupplyPartnerId, DemandPartnerId, TrafficType, RuleConditions) => Future[EstimatedAvailableQps] =
    (spId, dpId, dt, rc) =>
      estimateTask(spId, dpId, dt, rc)
        .run(estimateConfig)
        .runInFuture()

  private def estimateEndpointAvailableTask(
    spId: SupplyPartnerId,
    dpId: DemandPartnerId): ReaderT[Task, EstimateConfig, EstimatedAvailableQps] =
    humber.Estimate.estimateEndpointAvailable(spId, dpId)

  def estimateEndpointAvailable(estimateConfig: EstimateConfig):
  (RouterConfigurationId) => Future[EndpointAvailableForecast] =
    (rcId) =>
      estimateEndpointAvailableTask(rcId.spId, rcId.dpId)
        .map(qps => EndpointAvailableForecast(Some(qps)))
        .run(estimateConfig)
        .runInFuture()
}

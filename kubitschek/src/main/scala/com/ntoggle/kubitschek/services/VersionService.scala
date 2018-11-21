package com.ntoggle.kubitschek.services

import com.ntoggle.albi.{DesiredToggledQps, DemandPartnerId, SupplyPartnerId}
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.api.RuleSummary
import org.joda.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{\/, EitherT}
import scalaz.std.scalaFuture._
import scalaz.std.list._
import scalaz.std.option._
import scalaz.syntax.traverse._

object VersionService {

  private def versionRuleSummaryToRuleSummary(
    l: List[VersionRuleSummary],
    rc: RouterConfigurationId,
    actualsCount: (RouterConfigurationId, RuleId) => Future[MetricCount],
    availableForecast: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast])(implicit ctx: ExecutionContext): Future[List[RuleSummary]] = {
    l.traverse( vrs =>
      for {
        ac <- actualsCount(rc, vrs.id.ruleId)
        af <- availableForecast(rc.spId, rc.dpId, vrs.id.ruleId)
      } yield
      RuleSummary(vrs.id.ruleId, vrs.trafficType, vrs.desiredQps, ac, af, vrs.created, vrs.name)
    )
  }

  def create(
    newId: () => Future[String],
    now: () => Future[Instant],
    availableForecast: RouterConfigurationId => Future[EndpointAvailableForecast],
    requestCounts: RouterConfigurationId => Future[MetricCount],
    bidCounts: RouterConfigurationId => Future[MetricCount],
    ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount],
    ruleAvailable: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast],
    createEmptyVersion: (VersionId,RouterConfigurationId,CreatedInstant,DemandPartnerMaxQps) => Future[ConfigurationError \/ Version])
    (implicit ctx: ExecutionContext):
  (VersionCreateRequest) => ApiResponseFuture[VersionSummaryResponse] = {
    (vcr) =>
      for {
        now <- EitherT.right(now()).map(CreatedInstant.apply)
        nv <- EitherT.right(newId()).map(VersionId.apply)
        c = RouterConfigurationId(vcr.dpId,vcr.spId)
        af <- EitherT.right(availableForecast(c))
        rc <- EitherT.right(requestCounts(c))
        bc <- EitherT.right(bidCounts(c))
        vrs<- EitherT.right(versionRuleSummaryToRuleSummary(List[VersionRuleSummary](), c, ruleActuals, ruleAvailable))
        result <- EitherT.eitherT(createEmptyVersion(nv,c,now,vcr.maxQps)).leftMap(ConfigurationErrorRejections.rejection)

      } yield VersionSummaryResponse(
        result.id,
        result.routerConfigurationId.dpId,
        result.routerConfigurationId.spId,
        result.created,
        result.modified,
        result.published,
        result.maxQps,
        af,
        rc,
        bc,
        vrs
      )
  }

  def get(
    getVersion: VersionId => Future[Option[VersionSummary]],
    availableForecast: RouterConfigurationId => Future[EndpointAvailableForecast],
    requestCounts: RouterConfigurationId => Future[MetricCount],
    bidCounts: RouterConfigurationId => Future[MetricCount],
    ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount],
    ruleAvailable: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast])(implicit ctx: ExecutionContext):
  VersionId => Future[Option[VersionSummaryResponse]] = {
    id =>
      getVersion(id).flatMap(_.traverse(
        result =>
          for {
            af <- availableForecast(result.routerConfigurationId)
            rc <- requestCounts(result.routerConfigurationId)
            bc <- bidCounts(result.routerConfigurationId)
            vrs <- versionRuleSummaryToRuleSummary(result.rules, result.routerConfigurationId, ruleActuals, ruleAvailable)
          } yield
          VersionSummaryResponse(
            result.id,
            result.routerConfigurationId.dpId,
            result.routerConfigurationId.spId,
            result.created,
            result.modified,
            result.published,
            result.maxQps,
            af,
            rc,
            bc,
            vrs
          )))
  }

  def list(
    listVersions: (Option[DemandPartnerId], Option[SupplyPartnerId], Offset, Limit) =>
      Future[List[VersionSummary]],
    availableForecast: RouterConfigurationId => Future[EndpointAvailableForecast],
    requestCounts: RouterConfigurationId => Future[MetricCount],
    bidCounts: RouterConfigurationId => Future[MetricCount],
    ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount],
    ruleAvailable: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast])(implicit ctx: ExecutionContext):
  (Option[DemandPartnerId], Option[SupplyPartnerId], Offset, Limit) =>
    Future[List[VersionSummaryResponse]] =
    (dpId: Option[DemandPartnerId], spId: Option[SupplyPartnerId], o: Offset, l: Limit) =>

      listVersions(dpId, spId, o, l).flatMap(_.traverse { vs =>
        for {
          af <- availableForecast(vs.routerConfigurationId)
          rc <- requestCounts(vs.routerConfigurationId)
          bc <- bidCounts(vs.routerConfigurationId)
          vrs <- versionRuleSummaryToRuleSummary(vs.rules, vs.routerConfigurationId, ruleActuals, ruleAvailable)
        } yield
        VersionSummaryResponse(
          vs.id,
          vs.routerConfigurationId.dpId,
          vs.routerConfigurationId.spId,
          vs.created,
          vs.modified,
          vs.published,
          vs.maxQps,
          af,
          rc,
          bc,
          vrs
        )
      })


  def setQps(
    now: () => Future[Instant],
    setVersionQps: (VersionId, DemandPartnerMaxQps, Map[RuleId, DesiredToggledQps], ModifiedInstant) =>
      Future[ConfigurationError \/ VersionSummary],
    getVersion: VersionId => Future[Option[VersionSummaryResponse]],
    availableForecast: RouterConfigurationId => Future[EndpointAvailableForecast],
    requestCounts: RouterConfigurationId => Future[MetricCount],
    bidCounts: RouterConfigurationId => Future[MetricCount],
    ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount],
    ruleAvailable: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast])(implicit ctx: ExecutionContext):
  (VersionId, VersionQpsUpdateRequest) => ApiResponseFuture[VersionSummaryResponse] =
    (id, req) => {

      // persistence layer requires this info in Map, so convert it.

      val m: Map[RuleId, DesiredToggledQps] = (for {
        e <- req.rules
      } yield e.id -> e.desiredQps).toMap

      for {
        now <- EitherT.right(now()).map(ModifiedInstant.apply)
        r <- EitherT.eitherT(
          setVersionQps(id, req.maxQps, m, now)).leftMap(ConfigurationErrorRejections.rejection)
        af <- EitherT.right(availableForecast(r.routerConfigurationId))
        rc <- EitherT.right(requestCounts(r.routerConfigurationId))
        bc <- EitherT.right(bidCounts(r.routerConfigurationId))
        vrs <- EitherT.right(versionRuleSummaryToRuleSummary(r.rules, r.routerConfigurationId, ruleActuals, ruleAvailable))
      } yield VersionSummaryResponse(
        r.id,
        r.routerConfigurationId.dpId,
        r.routerConfigurationId.spId,
        r.created,
        r.modified,
        r.published,
        r.maxQps,
        af,
        rc,
        bc,
        vrs
      )
    }

  def copy(
    newId: () => Future[String],
    now: () => Future[Instant],
    copyVersion: (VersionId,VersionId,CreatedInstant) => Future[ConfigurationError \/ VersionSummary],
    availableForecast: RouterConfigurationId => Future[EndpointAvailableForecast],
    requestCounts: RouterConfigurationId => Future[MetricCount],
    bidCounts: RouterConfigurationId => Future[MetricCount],
    ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount],
    ruleAvailable: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast])(implicit ctx: ExecutionContext):
  VersionId => ApiResponseFuture[VersionSummaryResponse] =
    id => {
      for {
        initialVersion <- EitherT.right(newId()).map(VersionId.apply)
        now <- EitherT.right(now()).map(CreatedInstant.apply)
        r <- EitherT.eitherT(copyVersion(id, initialVersion, now)).leftMap(ConfigurationErrorRejections.rejection)
        af <- EitherT.right(availableForecast(r.routerConfigurationId))
        rc <- EitherT.right(requestCounts(r.routerConfigurationId))
        bc <- EitherT.right(bidCounts(r.routerConfigurationId))
        vrs <- EitherT.right(versionRuleSummaryToRuleSummary(r.rules, r.routerConfigurationId, ruleActuals, ruleAvailable))
      } yield VersionSummaryResponse(
        r.id,
        r.routerConfigurationId.dpId,
        r.routerConfigurationId.spId,
        r.created,
        r.modified,
        r.published,
        r.maxQps,
        af,
        rc,
        bc,
        vrs
      )
    }

  def publish(
    newId: () => Future[String],
    now: () => Future[Instant],
    publishVersion: (VersionId,PublishedInstant) => Future[ConfigurationError \/ VersionSummary],
    availableForecast: RouterConfigurationId => Future[EndpointAvailableForecast],
    requestCounts: RouterConfigurationId => Future[MetricCount],
    bidCounts: RouterConfigurationId => Future[MetricCount],
    ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount],
    ruleAvailable: (SupplyPartnerId, DemandPartnerId, RuleId) => Future[RuleAvailableForecast])
    (implicit ctx: ExecutionContext):  VersionId => ApiResponseFuture[VersionSummaryResponse] =
    id => {
      for {
        now <- EitherT.right(now()).map(CreatedInstant.apply)
        nv <- EitherT.right(newId()).map(VersionId.apply)
        pr <- EitherT.eitherT(publishVersion(id, PublishedInstant(now.value))).leftMap(ConfigurationErrorRejections.rejection)
        af <- EitherT.right(availableForecast(pr.routerConfigurationId))
        rc <- EitherT.right(requestCounts(pr.routerConfigurationId))
        bc <- EitherT.right(bidCounts(pr.routerConfigurationId))
        vrs <- EitherT.right(versionRuleSummaryToRuleSummary(pr.rules, pr.routerConfigurationId, ruleActuals, ruleAvailable))
      } yield VersionSummaryResponse(
        pr.id,
        pr.routerConfigurationId.dpId,
        pr.routerConfigurationId.spId,
        pr.created,
        pr.modified,
        pr.published,
        pr.maxQps,
        af,
        rc,
        bc,
        vrs
      )
    }

  def saveRule(
    saveRule: VersionId => Future[Option[VersionSummary]]):
  VersionId => Future[Option[VersionSummary]] = saveRule

  def removeRule(
    removeRule: (VersionId,RuleId) => Future[Option[VersionSummary]]):
  (VersionId, RuleId) => Future[Option[VersionSummary]] = (vId:VersionId,rId:RuleId) => removeRule(vId,rId)
}

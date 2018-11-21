package com.ntoggle.kubitschek.services

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import org.joda.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.std.scalaFuture._
import scalaz.{EitherT, \/}

object RuleService {

  private def createRule(
    id: RuleId, name: RuleName,
    trafficType: TrafficType, conditions: RuleConditions): Rule = {
    Rule(id, name, RuleCreatedInstant(Instant.now()), trafficType, conditions)
  }

  def toRuleConditionResponse[A](rc: RuleCondition[A])(f: A => String): RuleConditionResponse[A] =
    RuleConditionResponse(rc.default, rc.exceptions.map(a => RuleConditionPair(a, f(a))), rc.undefined)

  def toRuleConditionsResponse(rcs: RuleConditions): RuleConditionsResponse =
    RuleConditionsResponse(
      rcs.carrier.map(toRuleConditionResponse(_)(_.value)),
      // KUB-54 Handset name formatting temporarily here
      rcs.handset.map(toRuleConditionResponse(_)(id => s"${id.manufacturer.value} ${id.handset.value}")),
      rcs.deviceIdExistence.map(toRuleConditionResponse(_)(DeviceIdType.fold(_, _ => "No DeviceId", _ => "DeviceId"))),
      // KUB-54 Os name formatting temporarily here
      rcs.os.map(toRuleConditionResponse(_)(os => s"${os.name.value} ${os.version.version}")),
      rcs.wifi.map(toRuleConditionResponse(_)(ConnectionType.fold(_, _ => "No Wifi", _ => "Wifi"))),
      rcs.ipAddressExistence.map(toRuleConditionResponse(_)(IpAddressType.fold(_, _ => "No IP Address", _ => "IP Address"))),
      rcs.appAndSite.map(toRuleConditionResponse(_)(_.value)),
      rcs.banner.map(toRuleConditionResponse(_)(BannerType.fold(_, _ => "No Banner", _ => "Banner"))),
      rcs.mraid.map(toRuleConditionResponse(_)(MraidType.fold(_, _ => "No Mraid", _ => "Mraid"))),
      rcs.interstitial.map(toRuleConditionResponse(_)(InterstitialType.fold(_, _ => "No Interstitial", _ => "Interstitial"))),
      rcs.video.map(toRuleConditionResponse(_)(VideoType.fold(_, _ => "No Video", _ => "Video"))),
      rcs.native.map(toRuleConditionResponse(_)(NativeType.fold(_, _ => "No Native", _ => "Native"))),
      rcs.adSize.map(toRuleConditionResponse(_)(AdSizeId.toStringKey)),
      rcs.latlong.map(toRuleConditionResponse(_)(LatLongType.fold(_, _ => "No LatLong", _ => "LatLong"))),
      rcs.country.map(toRuleConditionResponse(_)(_.value)),
      rcs.region.map(toRuleConditionResponse(_)(RegionId.toStringKey)),
      rcs.city.map(toRuleConditionResponse(_)(_.value)),
      rcs.zip.map(toRuleConditionResponse(_)(_.value)),
      rcs.userList.map(toRuleConditionResponse(_)(_.value))
    )

  def toGetRuleResponse(r: Rule): GetRuleResponse =
    GetRuleResponse(
      r.id,
      r.name,
      r.created,
      r.trafficType,
      toRuleConditionsResponse(r.conditions))

  def get(getRule: RuleId => Future[Option[Rule]]):
  RuleId => Future[Option[GetRuleResponse]] =
    ruleId => getRule(ruleId).map(_.map(toGetRuleResponse))

  def listVersionsForRule(listVersions: (RuleId, DemandPartnerId, List[SupplyPartnerId], Offset, Limit) => Future[List[Version]]):
  (RuleId,DemandPartnerId,List[SupplyPartnerId], Offset, Limit) => Future[List[Version]] =
    (rId, dpId, spIds, offset, limit) => listVersions(rId, dpId, spIds, offset, limit)

  def save(
    newId: () => Future[String],
    saveRuleToVersion: (VersionId, Rule, DesiredToggledQps) => Future[ConfigurationError \/ Rule]
    )(implicit ctx: ExecutionContext):
  (VersionId, CreateRuleRequest) => ApiResponseFuture[GetRuleResponse] = {

    (vId, ruleReq) =>
      for {
        id <- EitherT.right(newId()).map(RuleId.apply)
        rule = createRule(id, ruleReq.name, ruleReq.trafficType, ruleReq.conditions)
        result <- EitherT.eitherT(saveRuleToVersion(vId, rule, DesiredToggledQps.Zero))
          .leftMap(ConfigurationErrorRejections.rejection)
      } yield toGetRuleResponse(result)
  }

  def replace(
    newId: () => Future[String],
    updateRuleInVersion: (VersionRuleId, Rule) => Future[ConfigurationError \/ Rule]
    )(implicit ctx: ExecutionContext):
  (VersionId, RuleId, ReplaceRuleRequest) => ApiResponseFuture[GetRuleResponse] = {

    (vId, rId, ruleReq) =>
      for {
        id <- EitherT.right(newId()).map(RuleId.apply)
        rule = createRule(id, ruleReq.name, ruleReq.trafficType, ruleReq.conditions)
        result <- EitherT.eitherT(updateRuleInVersion(VersionRuleId(vId, rId), rule))
          .leftMap(ConfigurationErrorRejections.rejection)
      } yield toGetRuleResponse(result)
  }
  def remove(
    removeRule: (VersionRuleId) => Future[ConfigurationError \/ Unit]
    ):
  (VersionRuleId) => ApiResponseFuture[Unit] =
    (existingId: VersionRuleId) =>
      for {
        result <- EitherT.eitherT(removeRule(existingId))
          .leftMap(ConfigurationErrorRejections.rejection)
      } yield result

  //EitherT.eitherT(removeRule(existingId).map(_.leftMap(ConfigurationErrorRejections.rejection)))

}
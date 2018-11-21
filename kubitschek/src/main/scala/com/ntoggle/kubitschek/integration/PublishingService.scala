package com.ntoggle.kubitschek.integration

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.albi.devices.{DeviceId, DeviceIp}
import com.ntoggle.helix.api.rules._
import com.ntoggle.helix.api.rules.etcd.EtcdClient
import com.ntoggle.helix.{api => helix}
import com.ntoggle.kubitschek.domain._

import scala.collection.immutable.Set
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.effect.Resource
import scalaz.std.list._
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scalaz.syntax.traverse._
import scalaz.{EitherT, \/}

object PublishingService {

  private def singleUseHelixRulesClient(
    ce: ConfigurationEndpoint, rc: RuleConfiguration): Future[Unit] = {
    val c = EtcdClient.withDefaultConfig(ce.asURI)
    val f = api.publish(c, rc)
    f.onComplete(_ => implicitly[Resource[EtcdClient]].close(c))
    f
  }

  private[integration] def toRuleConfiguration(
    vs: VersionSummary, rs: List[helix.rules.Rule]): RuleConfiguration =
    RuleConfiguration(vs.maxQps.value, rs)

  def publish(
    dbPublish: (VersionId, PublishedInstant) => Future[ConfigurationError \/ VersionSummary],
    getConfig: RouterConfigurationId => Future[Option[RouterConfiguration]],
    configPublish: (ConfigurationEndpoint, VersionSummary) => Future[Unit])(
    version: VersionId,
    instant: PublishedInstant): Future[ConfigurationError \/ VersionSummary] = {

    for {
      db <- EitherT.eitherT(dbPublish(version, instant))
      configOpt <- EitherT.right(getConfig(db.routerConfigurationId))
      config <- EitherT.eitherT(Future.successful(configOpt.\/>(ConfigurationError.routerConfigurationNotFound(db.routerConfigurationId))))
      pub <- EitherT.right(configPublish(config.target, db))
    } yield db
  }.run

  def publishConfiguration(
    getRule: com.ntoggle.kubitschek.domain.RuleId => Future[Option[com.ntoggle.kubitschek.domain.Rule]],
    getEstimatedQPS: (SupplyPartnerId, DemandPartnerId, com.ntoggle.kubitschek.domain.RuleId) => Future[RuleAvailableForecast])(
    target: ConfigurationEndpoint,
    version: VersionSummary): Future[Unit] = for {
    rulesAndQps <- version.rules.traverseU { (vrs: VersionRuleSummary) =>
      getRule(vrs.id.ruleId).map(_.map(vrs.desiredQps ->))
    }.map(_.flatten)
    converted <- rulesAndQps.traverseU { case (desiredQps, rule) =>
      getEstimatedQPS(
        version.routerConfigurationId.spId,
        version.routerConfigurationId.dpId, rule.id).map { estimatedQps =>
        // TODO: fix when we're in our own data center
        // val p = estimatedQps.oneDay.fold(PValue.Default)(PValue.calculateFromDesired(_, desiredQps))
        val p = PValue.Default
        convertRule(rule, p)
      }
    }
    _ <- singleUseHelixRulesClient(
      target, toRuleConfiguration(version, converted))
  } yield ()

  private[integration] def convertRule(
    rule: com.ntoggle.kubitschek.domain.Rule,
    pValue: PValue): com.ntoggle.helix.api.rules.Rule = {
    val cond = rule.conditions
    com.ntoggle.helix.api.rules.Rule(
      com.ntoggle.helix.api.rules.RuleId(rule.id.value),
      pValue,
      com.ntoggle.helix.api.rules.RuleConditions.builder(
        carrier = cond.carrier.map(a => convertCondition(a)),
        handset = cond.handset.map(a => convertCondition(a)),
        connectionType = cond.wifi.map(a => convertCondition(a)),
        app = cond.appAndSite.map(a => convertCondition(a)),
        adSize = cond.adSize.map(a => convertCondition(a)),
        country = cond.country.map(a => convertCondition(a)),
        region = cond.region.map(a => convertCondition(a)),
        city = cond.city.map(a => convertCondition(a)),
        zip = cond.zip.map(a => convertCondition(a)),
        os = cond.os.map(a => convertCondition(a)),
        latLong = cond.latlong.map { a =>
          checkPresence[com.ntoggle.albi.LatLong.type, LatLongType](a, com.ntoggle.kubitschek.domain.LatLong, NoLatLong)
        },
        deviceIp = cond.ipAddressExistence.map { a =>
          checkPresence[DeviceIp.type, IpAddressType](a, HasIpAddress, NoIpAddress)
        },
        deviceId = cond.deviceIdExistence.map { a =>
          checkPresence[DeviceId, DeviceIdType](a, HasDeviceId, NoDeviceId)
        },
        bannerType = cond.banner.map(a => convertCondition(a.map(mapBannerType))),
        mraidType = cond.mraid.map(a => convertCondition(a.map(mapMraidType))),
        interstitialType = cond.interstitial.map(a => convertCondition(a.map(mapInterstitialType))),
        videoType = cond.video.map(a => convertCondition(a.map(mapVideoType))),
        nativeType = cond.native.map(a => convertCondition(a.map(mapNativeType))),
        userListId = cond.userList.map(a => convertConditionWithoutUndefined(a.map(mapUserList)))
      )
    )
  }

  // We have chosen to have friendly 'name' on Helix RuleCondition. In Kubitschek, we have modeled 'UserListId' as a dbId
  // and use 'UserListName' as friendly name for API / UI on RuleCondition.
  private val mapUserList: UserListName => UserListId =
    a => UserListId(UserListName.valueLens.get(a))

  private val mapBannerType: BannerType => helix.BannerType =
    BannerType.fold(_, _ => helix.IsNotBanner, _ => helix.IsBanner)

  private val mapMraidType: MraidType => helix.MraidType =
    MraidType.fold(_, _ => helix.IsNotMraid, _ => helix.IsMraid)

  private val mapInterstitialType: InterstitialType => helix.InterstitialType =
    InterstitialType.fold(_, _ => helix.IsNotInterstitial, _ => helix.IsInterstitial)

  private val mapVideoType: VideoType => helix.VideoType =
    VideoType.fold(_, _ => helix.IsNotVideo, _ => helix.IsVideo)

  private val mapNativeType: NativeType => helix.NativeType =
    NativeType.fold(_, _ => helix.IsNotNative, _ => helix.IsNative)


  private def checkPresence[A, B](rc: RuleCondition[B], has: B, hasNot: B): AttributeCondition[A] = {
    rc.default match {
      case AllowAllDefaultConditionAction =>
        (rc.exceptions.contains(has), rc.exceptions.contains(hasNot)) match {
          case (false, false) =>
            AttributeCondition.allowAll
          case (true, false) =>
            IncludeUnknownAnd(Set.empty)
          case (false, true) =>
            AttributeCondition.allowAllIfKnown
          case (true, true) =>
            AttributeCondition.excludeAll
        }
      case ExcludeAllDefaultConditionAction =>
        (rc.exceptions.contains(has), rc.exceptions.contains(hasNot)) match {
          case (false, false) =>
            AttributeCondition.excludeAll
          case (true, false) =>
            AttributeCondition.allowAllIfKnown
          case (false, true) =>
            IncludeUnknownAnd(Set.empty)
          case (true, true) =>
            AttributeCondition.allowAll
        }
    }
  }

  private def convertCondition[A](rc: RuleCondition[A]): AttributeCondition[A] = {
    rc.default match {
        //black list
      case AllowAllDefaultConditionAction =>
          rc.undefined match {
            case AllowUndefinedConditionAction => ExcludeOnly(rc.exceptions)
            case ExcludeUndefinedConditionAction => ExcludeUnknownAnd(rc.exceptions)
          }
        // white list
      case ExcludeAllDefaultConditionAction =>
        rc.undefined match {
          case AllowUndefinedConditionAction => IncludeUnknownAnd(rc.exceptions)
          case ExcludeUndefinedConditionAction => IncludeOnly(rc.exceptions)
        }
    }
  }

  // Some attributes just don't care about the 'undefined' case.
  // For the black list case, this means that if attribute is not present, request is routed.
  // For the white list case, we route only if attribute is present and matched to a rule.
  private def convertConditionWithoutUndefined[A](rc: RuleCondition[A]): AttributeCondition[A] = {
    rc.default match {
      //black list
      case AllowAllDefaultConditionAction => ExcludeOnly(rc.exceptions)
      // white list
      case ExcludeAllDefaultConditionAction => IncludeOnly(rc.exceptions)
    }
  }

}

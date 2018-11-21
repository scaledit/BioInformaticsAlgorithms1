package com.ntoggle.kubitschek.estimation

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.humber.{estimation => humber}
import com.ntoggle.kubitschek.domain._
import shapeless.Poly1

object Mapping {

  private val toHumberDeviceIdType: DeviceIdType => humber.DeviceIdType = {
    case HasDeviceId => humber.HasDeviceId
    case NoDeviceId => humber.NoDeviceId
  }

  private val toHumberIpAddressType: IpAddressType => humber.IpAddressType = {
    case HasIpAddress => humber.HasIpAddress
    case NoIpAddress => humber.NoIpAddress
  }
  private val toHumberBannerType: BannerType => humber.IsBanner = {
    case IsBanner => humber.Banner
    case NotBanner => humber.NotBanner
  }
  private val toHumberMraidType: MraidType => humber.IsMraid = {
    case IsMraid => humber.Mraid
    case NotMraid => humber.NotMraid
  }
  private val toHumberInterstitialType: InterstitialType => humber.IsInterstitial = {
    case IsInterstitial => humber.Interstitial
    case NotInterstitial => humber.NotInterstitial
  }
  private val toHumberVideoType: VideoType => humber.IsVideo = {
    case IsVideo => humber.Video
    case NotVideo => humber.NotVideo
  }
  private val toHumberNativeType: NativeType => humber.IsNative = {
    case IsNative => humber.Native
    case NotNative => humber.NotNative
  }
  private val toHumberLatLongType: LatLongType => humber.LatLongType = {
    case com.ntoggle.kubitschek.domain.LatLong => humber.LatLong
    case com.ntoggle.kubitschek.domain.NoLatLong => humber.NoLatLong
  }

  private val toHumberDefaultAction: DefaultConditionAction => humber.DefaultConditionAction = {
    case AllowAllDefaultConditionAction => humber.DefaultConditionAction.Include
    case ExcludeAllDefaultConditionAction => humber.DefaultConditionAction.Exclude
  }
  private val toHumberUndefinedAction: UndefinedConditionAction => humber.UndefinedConditionAction = {
    case AllowUndefinedConditionAction => humber.IncludeUndefinedConditionAction
    case ExcludeUndefinedConditionAction => humber.ExcludeUndefinedConditionAction
  }
  private def mapSet[A, B](f: A => B): Set[A] => Set[B] = _.map(f)

  private def toHumberRuleConditionAB[A, B](f: Set[A] => Set[B]): RuleCondition[A] => humber.RuleCondition[B] =
    rc => humber.RuleCondition(
      toHumberDefaultAction(rc.default),
      f(rc.exceptions),
      toHumberUndefinedAction(rc.undefined))


  private def toHumberOptionRuleConditionAB[A, B](f: Set[A] => Set[B]): Option[RuleCondition[A]] => Option[humber.RuleCondition[B]] =
    _.map(toHumberRuleConditionAB[A, B](f))


  private object toHumberRuleCondition extends Poly1 {
    def caseIdentityMapping[A] =
      at[Option[RuleCondition[A]]](toHumberOptionRuleConditionAB(identity))
    def caseNonIdentityMapping[A, B](f: A => B) =
      at[Option[RuleCondition[A]]](toHumberOptionRuleConditionAB(mapSet(f)))

    implicit def caseTrafficType = caseIdentityMapping[TrafficType]
    implicit def caseCarrierId = caseIdentityMapping[CarrierId]
    implicit def caseHandsetId = caseIdentityMapping[HandsetId]
    implicit def caseDeviceIdType = caseNonIdentityMapping(toHumberDeviceIdType)
    implicit def caseOsId = caseIdentityMapping[OsId]
    implicit def caseConnectionType = caseIdentityMapping[ConnectionType]
    implicit def caseIpAddressType = caseNonIdentityMapping(toHumberIpAddressType)
    implicit def caseAppId = caseIdentityMapping[AppId]
    implicit def caseBannerType = caseNonIdentityMapping(toHumberBannerType)
    implicit def caseMraidType = caseNonIdentityMapping(toHumberMraidType)
    implicit def caseInterstitialType = caseNonIdentityMapping(toHumberInterstitialType)
    implicit def caseVideoType = caseNonIdentityMapping(toHumberVideoType)
    implicit def caseNativeType = caseNonIdentityMapping(toHumberNativeType)
    implicit def caseAdSizeId = caseIdentityMapping[AdSizeId]
    implicit def caseLatLongType = caseNonIdentityMapping(toHumberLatLongType)
    implicit def caseCountryIdAlpha2 = caseIdentityMapping[CountryIdAlpha2]
    implicit def caseRegionId = caseIdentityMapping[RegionId]
    implicit def caseCityName = caseIdentityMapping[CityName]
    implicit def caseZipAlpha = caseIdentityMapping[ZipAlpha]
    implicit def caseUserListName = caseIdentityMapping[UserListName]
  }

  val toHumberRuleConditions: (TrafficType, RuleConditions) => humber.RuleConditions = {
    (dt, rc) =>
      // use shapeless to apply mappings defined in operation above to each element in tuple
      import shapeless.syntax.std.tuple._
      val t = RuleConditions.unapply(rc).get
       .+:(Option(RuleCondition(DefaultConditionAction.Exclude, Set(dt), UndefinedConditionAction.Exclude)))
      (humber.RuleConditions.apply _).tupled(t.take(19).map(toHumberRuleCondition))

  }

  val toHumberToggledRuleConditions: ToggledRuleConditions => humber.ToggledRuleConditions = {
    rc =>
      humber.ToggledRuleConditions(
        rc.qps,
        toHumberRuleConditions(rc.trafficType, rc.conditions))
  }
}

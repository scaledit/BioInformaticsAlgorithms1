package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.AlbiGenerators._
import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.NGen
import com.ntoggle.kubitschek.api.{RulesQpsUpdate, VersionQpsUpdateRequest}
import org.scalacheck.{Arbitrary, Gen}
import scalaz.Apply
import scalaz.syntax.apply._
import scalaz.scalacheck.ScalaCheckBinding._

object DomainGenerators {

  def genPublishedInstant: Gen[PublishedInstant] =
    NGen.instant.map(PublishedInstant.apply)
  implicit def arbPublishedInstant: Arbitrary[PublishedInstant] =
    Arbitrary(genPublishedInstant)

  def genCreatedInstant: Gen[CreatedInstant] =
    NGen.instant.map(CreatedInstant.apply)
  implicit def arbCreatedInstant: Arbitrary[CreatedInstant] =
    Arbitrary(genCreatedInstant)

  def genModifiedInstant: Gen[ModifiedInstant] =
    NGen.instant.map(ModifiedInstant.apply)
  implicit def arbModifiedInstant: Arbitrary[ModifiedInstant] =
    Arbitrary(genModifiedInstant)

  def genRuleCreatedInstant: Gen[RuleCreatedInstant] =
    NGen.instant.map(RuleCreatedInstant.apply)
  implicit def arbRuleCreatedInstant: Arbitrary[RuleCreatedInstant] =
    Arbitrary(genRuleCreatedInstant)

  def genRuleName: Gen[RuleName] =
    NGen.utf8String.map(RuleName.apply)
  implicit def arbRuleName: Arbitrary[RuleName] =
    Arbitrary(genRuleName)

  def genDemandPartnerMaxQps: Gen[DemandPartnerMaxQps] =
    genMaxQps.map(DemandPartnerMaxQps.apply)
  implicit def arbDemandPartnerMaxQps: Arbitrary[DemandPartnerMaxQps] =
    Arbitrary(genDemandPartnerMaxQps)


  def genAttributeType: Gen[AttributeType] =
    Gen.oneOf(
      AppAttr,
      CountryAttr,
      HandsetAttr,
      RegionAttr,
      CityAttr,
      OsAttr,
      CarrierAttr)
  implicit def arbAttributeType: Arbitrary[AttributeType] =
    Arbitrary(genAttributeType)

  def genRouterConfigurationId: Gen[RouterConfigurationId] =
    ^(genDemandPartnerId,
      genSupplyPartnerId)(RouterConfigurationId.apply)
  implicit def arbRouterConfigurationId: Arbitrary[RouterConfigurationId] =
    Arbitrary(genRouterConfigurationId)

  def genVersionId: Gen[VersionId] =
    Gen.uuid.map(id => VersionId(id.toString))
  implicit def arbConfigurationVersion: Arbitrary[VersionId] =
    Arbitrary(genVersionId)

  def genRuleStatus: Gen[RuleStatus] =
    Gen.oneOf(RuleStatus.Enabled, RuleStatus.Disabled)
  implicit def arbRuleStatus: Arbitrary[RuleStatus] =
    Arbitrary(genRuleStatus)

  def genRuleId: Gen[RuleId] =
    Gen.uuid.map(id => RuleId(id.toString))
  implicit def arbRuleId: Arbitrary[RuleId] =
    Arbitrary(genRuleId)

  def genVersionRuleId: Gen[VersionRuleId] =
    ^(genVersionId, genRuleId)(VersionRuleId.apply)
  implicit def arbVersionRuleId: Arbitrary[VersionRuleId] =
    Arbitrary(genVersionRuleId)

  def genVersion: Gen[Version] =
    ^^^^^(genVersionId,
      genRouterConfigurationId,
      genCreatedInstant,
      genModifiedInstant,
      Gen.option(genPublishedInstant),
      genDemandPartnerMaxQps)(Version.apply)
  implicit def arbVersion: Arbitrary[Version] =
    Arbitrary(genVersion)

  def genVersionRuleSummary: Gen[VersionRuleSummary] =
    ^^^^(genVersionRuleId,
      TrafficType.genTrafficType,
      genDesiredToggledQps,
      genRuleCreatedInstant,
      genRuleName)(VersionRuleSummary.apply)
  implicit def arbVersionRuleSummary: Arbitrary[VersionRuleSummary] =
    Arbitrary(genVersionRuleSummary)

  def genVersionSummary: Gen[VersionSummary] =
    ^^^^^^(genVersionId,
      genRouterConfigurationId,
      genCreatedInstant,
      genModifiedInstant,
      Gen.option(genPublishedInstant),
      genDemandPartnerMaxQps,
      NGen.listOfMaxSize(10, genVersionRuleSummary))(VersionSummary.apply)
  implicit def arbVersionSummary: Arbitrary[VersionSummary] =
    Arbitrary(genVersionSummary)

  def genRuleQpsUpdate: Gen[RulesQpsUpdate] =
    ^(genRuleId,
      genDesiredToggledQps)(RulesQpsUpdate.apply)
  implicit def arbRuleQpsUpdate: Arbitrary[RulesQpsUpdate] =
    Arbitrary(genRuleQpsUpdate)

  def genQpsUpdateRequest: Gen[VersionQpsUpdateRequest] =
    ^(genDemandPartnerMaxQps,
      NGen.listOfMaxSize(10, genRuleQpsUpdate))(VersionQpsUpdateRequest.apply)
  implicit def arbVersionQpsUpdate: Arbitrary[VersionQpsUpdateRequest] =
    Arbitrary(genQpsUpdateRequest)

  def genUndefinedConditionAction: Gen[UndefinedConditionAction] =
    Gen.oneOf(
      UndefinedConditionAction.Allow,
      UndefinedConditionAction.Exclude)
  implicit def arbUndefinedConditionAction: Arbitrary[UndefinedConditionAction] =
    Arbitrary(genUndefinedConditionAction)

  def genDefaultConditionAction: Gen[DefaultConditionAction] =
    Gen.oneOf(
      DefaultConditionAction.Allow,
      DefaultConditionAction.Exclude)

  implicit def arbDefaultConditionAction: Arbitrary[DefaultConditionAction] =
    Arbitrary(genDefaultConditionAction)

  def genRuleCondition[A](genA: Gen[A]): Gen[RuleCondition[A]] =
    ^^(genDefaultConditionAction,
      NGen.containerOfSizeRanged[Set, A](0, 5, genA),
      genUndefinedConditionAction)(RuleCondition.apply)

  def genLatLongType: Gen[LatLongType] = Gen.oneOf(LatLong, NoLatLong)
  implicit def arbLatLongType: Arbitrary[LatLongType] = Arbitrary(genLatLongType)

  def genDeviceIdType: Gen[DeviceIdType] = Gen.oneOf(HasDeviceId, NoDeviceId)
  implicit def arbDeviceIdType: Arbitrary[DeviceIdType] = Arbitrary(genDeviceIdType)

  def genIpAddressType: Gen[IpAddressType] = Gen.oneOf(HasIpAddress, NoIpAddress)
  implicit def arbIpAddressType: Arbitrary[IpAddressType] = Arbitrary(genIpAddressType)

  def genBannerType: Gen[BannerType] = Gen.oneOf(IsBanner, NotBanner)
  implicit def arbBannerType: Arbitrary[BannerType] = Arbitrary(genBannerType)

  def genMraidType: Gen[MraidType] = Gen.oneOf(IsMraid, NotMraid)
  implicit def arbMraidType: Arbitrary[MraidType] = Arbitrary(genMraidType)

  def genInterstitialType: Gen[InterstitialType] = Gen.oneOf(IsInterstitial, NotInterstitial)
  implicit def arbInterstitialType: Arbitrary[InterstitialType] = Arbitrary(genInterstitialType)

  def genVideoType: Gen[VideoType] = Gen.oneOf(IsVideo, NotVideo)
  implicit def arbVideoType: Arbitrary[VideoType] = Arbitrary(genVideoType)

  def genNativeType: Gen[NativeType] = Gen.oneOf(IsNative, NotNative)
  implicit def arbNativeType: Arbitrary[NativeType] = Arbitrary(genNativeType)

  def apply18[F[_], A, B, C, D, E, FF, G, H, I, J, K, L, M, N, O, P, Q, RR, R](
    fa: => F[A], fb: => F[B], fc: => F[C], fd: => F[D], fe: => F[E],
    ff: => F[FF], fg: => F[G], fh: => F[H], fi: => F[I], fj: => F[J],
    fk: => F[K], fl: => F[L], fm: => F[M], fn: => F[N], fo: => F[O],
    fp: => F[P], fq: => F[Q], fr: => F[RR])
    (f: (A, B, C, D, E, FF, G, H, I, J, K, L, M, N, O, P, Q, RR) => R)
    (implicit F: Apply[F]): F[R] =
    F.apply4(
      F.tuple5(fa, fb, fc, fd, fe),
      F.tuple5(ff, fg, fh, fi, fj),
      F.tuple5(fk, fl, fm, fn, fo),
      F.tuple3(fp, fq, fr))((t, t2, t3, t4) =>
      f(t._1, t._2, t._3, t._4, t._5,
        t2._1, t2._2, t2._3, t2._4, t2._5,
        t3._1, t3._2, t3._3, t3._4, t3._5,
        t4._1, t4._2, t4._3))

  def apply19[F[_], A, B, C, D, E, FF, G, H, I, J, K, L, M, N, O, P, Q, RR, S, R](
    fa: => F[A], fb: => F[B], fc: => F[C], fd: => F[D], fe: => F[E],
    ff: => F[FF], fg: => F[G], fh: => F[H], fi: => F[I], fj: => F[J],
    fk: => F[K], fl: => F[L], fm: => F[M], fn: => F[N], fo: => F[O],
    fp: => F[P], fq: => F[Q], fr: => F[RR], fs: => F[S])
    (f: (A, B, C, D, E, FF, G, H, I, J, K, L, M, N, O, P, Q, RR, S) => R)
    (implicit F: Apply[F]): F[R] =
    F.apply4(
      F.tuple5(fa, fb, fc, fd, fe),
      F.tuple5(ff, fg, fh, fi, fj),
      F.tuple5(fk, fl, fm, fn, fo),
      F.tuple4(fp, fq, fr, fs))((t, t2, t3, t4) =>
      f(t._1, t._2, t._3, t._4, t._5,
        t2._1, t2._2, t2._3, t2._4, t2._5,
        t3._1, t3._2, t3._3, t3._4, t3._5,
        t4._1, t4._2, t4._3, t4._4))

  def apply20[F[_], A, B, C, D, E, FF, G, H, I, J, K, L, M, N, O, P, Q, RR, S, T, R](
    fa: => F[A], fb: => F[B], fc: => F[C], fd: => F[D], fe: => F[E],
    ff: => F[FF], fg: => F[G], fh: => F[H], fi: => F[I], fj: => F[J],
    fk: => F[K], fl: => F[L], fm: => F[M], fn: => F[N], fo: => F[O],
    fp: => F[P], fq: => F[Q], fr: => F[RR], fs: => F[S], ft: => F[T])
    (f: (A, B, C, D, E, FF, G, H, I, J, K, L, M, N, O, P, Q, RR, S, T) => R)
    (implicit F: Apply[F]): F[R] =
    F.apply4(
      F.tuple5(fa, fb, fc, fd, fe),
      F.tuple5(ff, fg, fh, fi, fj),
      F.tuple5(fk, fl, fm, fn, fo),
      F.tuple5(fp, fq, fr, fs, ft))((t, t2, t3, t4) =>
      f(t._1, t._2, t._3, t._4, t._5,
        t2._1, t2._2, t2._3, t2._4, t2._5,
        t3._1, t3._2, t3._3, t3._4, t3._5,
        t4._1, t4._2, t4._3, t4._4, t4._5))


  def genUserListId: Gen[UserListId] = genId(UserListId.apply)
  implicit def arbUserListId: Arbitrary[UserListId] = Arbitrary(genUserListId)

  def genUserListName: Gen[UserListName] = NGen.utf8String.map(UserListName.apply)
  implicit def arbUserListName: Arbitrary[UserListName] = Arbitrary(genUserListName)

  def genRuleConditions: Gen[RuleConditions] =
    apply19(
      Gen.option(genRuleCondition(genCarrierId)),
      Gen.option(genRuleCondition(genHandsetId)),
      Gen.option(genRuleCondition(genDeviceIdType)),
      Gen.option(genRuleCondition(genOsId)),
      Gen.option(genRuleCondition(genConnectionType)),
      Gen.option(genRuleCondition(genIpAddressType)),
      Gen.option(genRuleCondition(genAppId)),
      Gen.option(genRuleCondition(genBannerType)),
      Gen.option(genRuleCondition(genMraidType)),
      Gen.option(genRuleCondition(genInterstitialType)),
      Gen.option(genRuleCondition(genVideoType)),
      Gen.option(genRuleCondition(genNativeType)),
      Gen.option(genRuleCondition(genAdSizeId)),
      Gen.option(genRuleCondition(genLatLongType)),
      Gen.option(genRuleCondition(genCountryIdAlpha2)),
      Gen.option(genRuleCondition(genRegionId)),
      Gen.option(genRuleCondition(genCityName)),
      Gen.option(genRuleCondition(genZipAlpha)),
      Gen.option(genRuleCondition(genUserListName)))(RuleConditions.apply)
}


package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.albi.{LatLong => _, _}
import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import com.ntoggle.kubitschek.domain._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.std.list._
import scalaz.syntax.foldable1._

private[sql] case class SqlRuleConditions(
  values: List[SqlRuleCondition],
  exceptions: List[SqlRuleConditionException])
private[sql] object SqlRuleConditions
  extends LazyLogging
  with EqualzAndShowz[SqlRuleConditions] {

  val Empty = SqlRuleConditions(
    List.empty,
    List.empty)

  implicit val monoidSqlRuleConditions: Monoid[SqlRuleConditions] =
    new Monoid[SqlRuleConditions] {
      def zero: SqlRuleConditions = Empty
      def append(
        f1: SqlRuleConditions,
        f2: => SqlRuleConditions): SqlRuleConditions =
        SqlRuleConditions(
          f1.values ::: f2.values,
          f1.exceptions ::: f2.exceptions)
    }

  val valuesLens: SqlRuleConditions @> List[SqlRuleCondition] =
    Lens.lensu((c, v) => c.copy(values = v), _.values)

  val exceptionsLens: SqlRuleConditions @> List[SqlRuleConditionException] =
    Lens.lensu((c, v) => c.copy(exceptions = v), _.exceptions)

  object SqlLatLongType {
    object Keys {
      val LatLong = "latLong"
      val NoLatLong = "noLatLong"
    }
    val toStringKey: LatLongType => String =
      LatLongType.fold(_, _ => Keys.NoLatLong, _ => Keys.LatLong)
    val fromStringKey: String => Option[LatLongType] = {
      case Keys.NoLatLong => Option(NoLatLong)
      case Keys.LatLong => Option(com.ntoggle.kubitschek.domain.LatLong)
      case _ => None
    }
  }
  object SqlConnectionType {
    object Keys {
      val Wifi = "wifi"
      val NotWifi = "notWifi"
    }
    val toStringKey: ConnectionType => String =
      ConnectionType.fold(_, _ => Keys.NotWifi, _ => Keys.Wifi)
    val fromStringKey: String => Option[ConnectionType] = {
      case Keys.NotWifi => Option(NotWifi)
      case Keys.Wifi => Option(Wifi)
      case _ => None
    }
  }

  object SqlDeviceIdType {
    object Keys {
      val DeviceId = "deviceId"
      val NoDeviceId = "noDeviceId"
    }
    val toStringKey: DeviceIdType => String =
      DeviceIdType.fold(_, _ => Keys.NoDeviceId, _ => Keys.DeviceId)
    val fromStringKey: String => Option[DeviceIdType] = {
      case Keys.NoDeviceId => Option(NoDeviceId)
      case Keys.DeviceId => Option(HasDeviceId)
      case _ => None
    }
  }

  object SqlIpAddressType {
    object Keys {
      val IpAddress = "ipAddress"
      val NoIpAddress = "noIpAddress"
    }
    val toStringKey: IpAddressType => String =
      IpAddressType.fold(_, _ => Keys.NoIpAddress, _ => Keys.IpAddress)
    val fromStringKey: String => Option[IpAddressType] = {
      case Keys.NoIpAddress => Option(NoIpAddress)
      case Keys.IpAddress => Option(HasIpAddress)
      case _ => None
    }
  }

  object SqlBannerType {
    object Keys {
      val Banner = "banner"
      val NoBanner = "noBanner"
    }
    val toStringKey: BannerType => String =
      BannerType.fold(_, _ => Keys.NoBanner, _ => Keys.Banner)
    val fromStringKey: String => Option[BannerType] = {
      case Keys.NoBanner => Option(NotBanner)
      case Keys.Banner => Option(IsBanner)
      case _ => None
    }
  }

  object SqlMraidType {
    object Keys {
      val Mraid = "mraid"
      val NoMraid = "noMraid"
    }
    val toStringKey: MraidType => String =
      MraidType.fold(_, _ => Keys.NoMraid, _ => Keys.Mraid)
    val fromStringKey: String => Option[MraidType] = {
      case Keys.NoMraid => Option(NotMraid)
      case Keys.Mraid => Option(IsMraid)
      case _ => None
    }
  }

  object SqlInterstitialType {
    object Keys {
      val Interstitial = "interstitial"
      val NoInterstitial = "noInterstitial"
    }
    val toStringKey: InterstitialType => String =
      InterstitialType.fold(_, _ => Keys.NoInterstitial, _ => Keys.Interstitial)
    val fromStringKey: String => Option[InterstitialType] = {
      case Keys.NoInterstitial => Option(NotInterstitial)
      case Keys.Interstitial => Option(IsInterstitial)
      case _ => None
    }
  }
  
  object SqlVideoType {
    object Keys {
      val Video = "video"
      val NoVideo = "noVideo"
    }
    val toStringKey: VideoType => String =
      VideoType.fold(_, _ => Keys.NoVideo, _ => Keys.Video)
    val fromStringKey: String => Option[VideoType] = {
      case Keys.NoVideo => Option(NotVideo)
      case Keys.Video => Option(IsVideo)
      case _ => None
    }
  }

  object SqlNativeType {
    object Keys {
      val Native = "native"
      val NoNative = "noNative"
    }
    val toStringKey: NativeType => String =
      NativeType.fold(_, _ => Keys.NoNative, _ => Keys.Native)
    val fromStringKey: String => Option[NativeType] = {
      case Keys.NoNative => Option(NotNative)
      case Keys.Native => Option(IsNative)
      case _ => None
    }
  }
  
  private def setRuleCondition(
    condition: SqlRuleCondition,
    exceptions: List[SqlRuleConditionException]): State[RuleConditions, Unit] = {

    def ruleCondition[A](f: String => Option[A]): RuleCondition[A] =
      RuleCondition(
        SqlRuleConditionActionType.toDefault(
          condition.defaultAction),
        exceptions.flatMap(e => f(e.value).toList).toSet,
        SqlRuleConditionActionType.toUndefined(
          condition.undefinedAction))

    condition.attributeType match {
      case ConditionType.Carrier(a) => RuleConditions.carrierLens %== (
        _ => Option(ruleCondition(s => Option(CarrierId(s)))))
      case ConditionType.Handset(a) => RuleConditions.handsetLens %== (
        _ => Option(ruleCondition(HandsetId.fromStringKey)))
      case ConditionType.DeviceIdType(a) => RuleConditions.deviceIdLens %== (
        _ => Option(ruleCondition(SqlDeviceIdType.fromStringKey)))
      case ConditionType.OS(a) => RuleConditions.osLens %== (
        _ => Option(ruleCondition(OsId.fromStringKey)))
      case ConditionType.ConnectionType(a) => RuleConditions.wifiLens %== (
        _ => Option(ruleCondition(SqlConnectionType.fromStringKey)))
      case ConditionType.IpAddressType(a) => RuleConditions.ipAddressLens %== (
        _ => Option(ruleCondition(SqlIpAddressType.fromStringKey)))
      case ConditionType.App(a) => RuleConditions.appLens %== (_ => Option(
        ruleCondition(s => Option(AppId(s)))))
      case ConditionType.BannerType(a) => RuleConditions.bannerLens %== (
        _ => Option(ruleCondition(SqlBannerType.fromStringKey)))
      case ConditionType.MraidType(a) => RuleConditions.mraidLens %== (
        _ => Option(ruleCondition(SqlMraidType.fromStringKey)))
      case ConditionType.InterstitialType(a) => RuleConditions.interstitialLens %== (
        _ => Option(ruleCondition(SqlInterstitialType.fromStringKey)))
      case ConditionType.VideoType(a) => RuleConditions.videoLens %== (
        _ => Option(ruleCondition(SqlVideoType.fromStringKey)))
      case ConditionType.NativeType(a) => RuleConditions.nativeLens %== (
        _ => Option(ruleCondition(SqlNativeType.fromStringKey)))
      case ConditionType.AdSize(a) => RuleConditions.adSizeLens %== (
        _ => Option(ruleCondition(AdSizeId.fromStringKey)))
      case ConditionType.LatLong(a) => RuleConditions.latLongLens %== (
        _ => Option(ruleCondition(SqlLatLongType.fromStringKey)))
      case ConditionType.Country(a) => RuleConditions.countryLens %== (
        _ => Option(ruleCondition(CountryIdAlpha2.fromString)))
        //TODO need to validate RegionId.fromStringKey
      case ConditionType.Region(a) => RuleConditions.regionLens %== (
        _ => Option(ruleCondition(s => Some(RegionId.fromStringKey(s)))))
      case ConditionType.City(a) => RuleConditions.cityLens %== (
        _ => Option(ruleCondition(s => Option(CityName(s)))))
      case ConditionType.Zip(a) => RuleConditions.zipLens %== (
        _ => Option(ruleCondition(s => Option(ZipAlpha(s)))))
      case ConditionType.UserList(a) => RuleConditions.userListLens %== (
        _ => Option(ruleCondition(s => Option(UserListName(s)))))
      case unknown =>
        logger.warn(s"Unable to map rule condition with type '${unknown.value}'")
        State(s => (s, ()))
    }
  }

  def toRuleConditions(
    conditions: SqlRuleConditions): Map[RuleId, RuleConditions] = {
    val exceptions = conditions.exceptions.groupBy(_.ruleConditionId)
    conditions.values
      .groupBy1(_.ruleId)
      .mapValues {
      _.traverseS_ { c =>
        setRuleCondition(
          c,
          exceptions.getOrElse(c.id, List.empty))
      }.exec(RuleConditions.Empty)
    }
  }

  def fromRuleConditions(
    ruleId: RuleId,
    conditions: RuleConditions,
    newId: () => Future[SqlRuleConditionId])(
    implicit ctx: ExecutionContext): Future[SqlRuleConditions] = {
    import scalaz.syntax.monad._
    def addSqlCondition[A: ConditionType](
      c: RuleCondition[A])(f: A => String): StateT[Future, SqlRuleConditions, Unit] =
      for {
        id <- newId().liftM[({type l[g[_], a] = StateT[g, SqlRuleConditions, a]})#l]
        _ <- (SqlRuleConditions.valuesLens %== {
          SqlRuleCondition(
            id,
            ruleId,
            implicitly[ConditionType[A]].conditionType,
            SqlRuleConditionActionType.fromDefault(c.default),
            SqlRuleConditionActionType.fromUndefined(c.undefined)) :: _
        }).lift[Future]

        _ <- (SqlRuleConditions.exceptionsLens %== { exceptions =>
          c.exceptions.toList
            .map(e => SqlRuleConditionException(id, f(e))) ::: exceptions
        }).lift[Future]
      } yield ()

    val s: StateT[Future, SqlRuleConditions, Unit] = for {
      _ <- conditions.carrier.traverseU_(addSqlCondition(_)(_.value))
      _ <- conditions.handset
        .traverseU_(addSqlCondition(_)(HandsetId.toStringKey))
      _ <- conditions.deviceIdExistence
        .traverseU_(addSqlCondition(_)(SqlDeviceIdType.toStringKey))
      _ <- conditions.os
        .traverseU_(addSqlCondition(_)(OsId.toStringKey))
      _ <- conditions.wifi
        .traverseU_(addSqlCondition(_)(SqlConnectionType.toStringKey))
      _ <- conditions.ipAddressExistence
        .traverseU_(addSqlCondition(_)(SqlIpAddressType.toStringKey))
      _ <- conditions.appAndSite.traverseU_(addSqlCondition(_)(_.value))
      _ <- conditions.banner
        .traverseU_(addSqlCondition(_)(SqlBannerType.toStringKey))
      _ <- conditions.mraid
        .traverseU_(addSqlCondition(_)(SqlMraidType.toStringKey))
      _ <- conditions.interstitial
        .traverseU_(addSqlCondition(_)(SqlInterstitialType.toStringKey))
      _ <- conditions.video
        .traverseU_(addSqlCondition(_)(SqlVideoType.toStringKey))
      _ <- conditions.native
        .traverseU_(addSqlCondition(_)(SqlNativeType.toStringKey))
      _ <- conditions.adSize
        .traverseU_(addSqlCondition(_)(AdSizeId.toStringKey))
      _ <- conditions.latlong
        .traverseU_(addSqlCondition(_)(SqlLatLongType.toStringKey))
      _ <- conditions.country.traverseU_(addSqlCondition(_)(_.value))
      _ <- conditions.region.traverseU_(addSqlCondition(_)(RegionId.toStringKey))
      _ <- conditions.city.traverseU_(addSqlCondition(_)(_.value))
      _ <- conditions.zip.traverseU_(addSqlCondition(_)(_.value))
      _ <- conditions.userList.traverseU_(addSqlCondition(_)(_.value))
    } yield ()
    s.exec(Empty)
  }

}

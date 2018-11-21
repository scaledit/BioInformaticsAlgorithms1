package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.{LatLong => _, _}
import com.ntoggle.goldengate.playjson.FormatAsString
import com.ntoggle.goldengate.scalazint.{Showz, EqualzAndShowz, OrderzByString}
import org.joda.time.Instant
import com.ntoggle.goldengate.playjson.PlayJsonUtils
import com.ntoggle.goldengate.playjson.PlayJsonSyntax._
import com.ntoggle.goldengate.playjson.PlayFormat._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.syntax.std.option._
import scalaz._

sealed trait RuleStatus
object RuleStatus {
  val Enabled: RuleStatus = RuleEnabled
  val Disabled: RuleStatus = RuleDisabled

  def fromStringValue(s: String): Option[RuleStatus] =
    s match {
      case "enabled" => Some(RuleStatus.Enabled)
      case "disabled" => Some(RuleStatus.Disabled)
      case _ => None
    }

  def toStringValue(s: RuleStatus): String =
    s match {
      case RuleEnabled => "enabled"
      case RuleDisabled => "disabled"
    }

  def fromBoolean(enabled: Boolean): RuleStatus =
    if (enabled) Enabled else Disabled

  def toBoolean(rs: RuleStatus): Boolean = equalRuleStatus.equal(rs, Enabled)

  implicit val format: Format[RuleStatus] = {

    val writes: Writes[RuleStatus] =
      implicitly[Writes[String]].contramap((s: RuleStatus) => toStringValue(s))

    val reads = Reads[RuleStatus] {
      case JsString(s) => fromStringValue(s).\/>("Invalid rule status").toJsResult
      case other => PlayJsonUtils.jsError("Rule status must be string")
    }

    Format(reads, writes)
  }

  implicit val equalRuleStatus: Equal[RuleStatus] = Equal.equalA
  implicit val showRuleStatus: Show[RuleStatus] = Show.showFromToString
}


case object RuleEnabled extends RuleStatus
case object RuleDisabled extends RuleStatus

case class RuleId(value: String) extends AnyVal
object RuleId
  extends OrderzByString[RuleId]
  with Showz[RuleId] {
  val readRuleId: Reads[RuleId] =
    Reads.uuidReader(true).map(u => RuleId(u.toString))
  val writeRuleId: Writes[RuleId] =
    implicitly[Writes[String]].contramap(ruleId => ruleId.value)

  implicit val formatRuleId: Format[RuleId] =
    Format(readRuleId, writeRuleId)
}

case class RuleName(value: String) extends AnyVal
object RuleName extends FormatAsString[RuleName] {
  val valueLens: RuleName @> String =
    Lens.lensu((rn, v) => rn.copy(value = v), _.value)
}

case class RuleCreatedInstant(value: Instant) extends AnyVal
object RuleCreatedInstant {

  val valueLens: RuleCreatedInstant @> Instant =
    Lens.lensu((rc, v) => rc.copy(value = v), _.value)

  import InstantJson._
  implicit val formatRuleCreatedInstant: Format[RuleCreatedInstant] =
    implicitly[Format[Instant]].inmap(RuleCreatedInstant.apply, _.value)
  import com.ntoggle.goldengate.jodaext.Implicits._
  implicit val orderRuleCreatedInstant: Order[RuleCreatedInstant] =
    Order[Instant].contramap(_.value)
}

sealed trait LatLongType
object LatLongType extends EqualzAndShowz[LatLongType] {
  def fold[A](
    value: LatLongType,
    fNoLatLong: NoLatLong.type => A,
    fLatLong: LatLong.type => A): A = value match {
    case NoLatLong => fNoLatLong(NoLatLong)
    case LatLong => fLatLong(LatLong)
  }

  implicit val formats: Format[LatLongType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) LatLong else NoLatLong,
      b => fold(b, _ => false, _ => true))
}
case object LatLong extends LatLongType
case object NoLatLong extends LatLongType

sealed trait IpAddressType
object IpAddressType extends EqualzAndShowz[IpAddressType] {
  def fold[A](
    value: IpAddressType,
    fNoIpAddress: NoIpAddress.type => A,
    fIpAddress: HasIpAddress.type => A): A = value match {
    case NoIpAddress => fNoIpAddress(NoIpAddress)
    case HasIpAddress => fIpAddress(HasIpAddress)
  }

  implicit val formats: Format[IpAddressType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) HasIpAddress else NoIpAddress,
      b => fold(b, _ => false, _ => true))
}
case object HasIpAddress extends IpAddressType
case object NoIpAddress extends IpAddressType

sealed trait DeviceIdType
object DeviceIdType extends EqualzAndShowz[DeviceIdType] {
  def fold[A](
    value: DeviceIdType,
    fNoDeviceId: NoDeviceId.type => A,
    fDeviceId: HasDeviceId.type => A): A = value match {
    case NoDeviceId => fNoDeviceId(NoDeviceId)
    case HasDeviceId => fDeviceId(HasDeviceId)
  }

  implicit val formats: Format[DeviceIdType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) HasDeviceId else NoDeviceId,
      b => fold(b, _ => false, _ => true))
}
case object HasDeviceId extends DeviceIdType
case object NoDeviceId extends DeviceIdType

sealed trait BannerType
object BannerType extends EqualzAndShowz[BannerType] {
  def fold[A](
    value: BannerType,
    fNoBanner: NotBanner.type => A,
    fBanner: IsBanner.type => A): A = value match {
    case NotBanner => fNoBanner(NotBanner)
    case IsBanner => fBanner(IsBanner)
  }

  implicit val formats: Format[BannerType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) IsBanner else NotBanner,
      b => fold(b, _ => false, _ => true))
}
case object IsBanner extends BannerType
case object NotBanner extends BannerType

sealed trait MraidType
object MraidType extends EqualzAndShowz[MraidType] {
  def fold[A](
    value: MraidType,
    fNoMraid: NotMraid.type => A,
    fMraid: IsMraid.type => A): A = value match {
    case NotMraid => fNoMraid(NotMraid)
    case IsMraid => fMraid(IsMraid)
  }

  implicit val formats: Format[MraidType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) IsMraid else NotMraid,
      b => fold(b, _ => false, _ => true))
}
case object IsMraid extends MraidType
case object NotMraid extends MraidType

sealed trait InterstitialType
object InterstitialType extends EqualzAndShowz[InterstitialType] {
  def fold[A](
    value: InterstitialType,
    fNoInterstitial: NotInterstitial.type => A,
    fInterstitial: IsInterstitial.type => A): A = value match {
    case NotInterstitial => fNoInterstitial(NotInterstitial)
    case IsInterstitial => fInterstitial(IsInterstitial)
  }

  implicit val formats: Format[InterstitialType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) IsInterstitial else NotInterstitial,
      b => fold(b, _ => false, _ => true))
}
case object IsInterstitial extends InterstitialType
case object NotInterstitial extends InterstitialType

sealed trait VideoType
object VideoType extends EqualzAndShowz[VideoType] {
  def fold[A](
    value: VideoType,
    fNoVideo: NotVideo.type => A,
    fVideo: IsVideo.type => A): A = value match {
    case NotVideo => fNoVideo(NotVideo)
    case IsVideo => fVideo(IsVideo)
  }

  implicit val formats: Format[VideoType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) IsVideo else NotVideo,
      b => fold(b, _ => false, _ => true))
}
case object IsVideo extends VideoType
case object NotVideo extends VideoType

sealed trait NativeType
object NativeType extends EqualzAndShowz[NativeType] {
  def fold[A](
    value: NativeType,
    fNoNative: NotNative.type => A,
    fNative: IsNative.type => A): A = value match {
    case NotNative => fNoNative(NotNative)
    case IsNative => fNative(IsNative)
  }

  implicit val formats: Format[NativeType] =
    implicitly[Format[Boolean]].inmap(
      a => if (a) IsNative else NotNative,
      b => fold(b, _ => false, _ => true))
}
case object IsNative extends NativeType
case object NotNative extends NativeType


case class RuleConditions(
  carrier: Option[RuleCondition[CarrierId]],
  handset: Option[RuleCondition[HandsetId]],
  deviceIdExistence: Option[RuleCondition[DeviceIdType]],
  os: Option[RuleCondition[OsId]],
  wifi: Option[RuleCondition[ConnectionType]],
  ipAddressExistence: Option[RuleCondition[IpAddressType]],
  appAndSite: Option[RuleCondition[AppId]],
  banner: Option[RuleCondition[BannerType]],
  mraid: Option[RuleCondition[MraidType]],
  interstitial: Option[RuleCondition[InterstitialType]],
  video: Option[RuleCondition[VideoType]],
  native: Option[RuleCondition[NativeType]],
  adSize: Option[RuleCondition[AdSizeId]],
  latlong: Option[RuleCondition[LatLongType]],
  country: Option[RuleCondition[CountryIdAlpha2]],
  region: Option[RuleCondition[RegionId]],
  city: Option[RuleCondition[CityName]],
  zip: Option[RuleCondition[ZipAlpha]],
  userList: Option[RuleCondition[UserListName]])
object RuleConditions extends EqualzAndShowz[RuleConditions] {
  val Empty =
    RuleConditions(None, None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None)

  val userListLens: RuleConditions @> Option[RuleCondition[UserListName]] =
    Lens.lensu((rc, v) => rc.copy(userList = v), _.userList)


  val carrierLens: RuleConditions @> Option[RuleCondition[CarrierId]] =
    Lens.lensu((rc, v) => rc.copy(carrier = v), _.carrier)

  val handsetLens: RuleConditions @> Option[RuleCondition[HandsetId]] =
    Lens.lensu((rc, v) => rc.copy(handset = v), _.handset)

  val deviceIdLens: RuleConditions @> Option[RuleCondition[DeviceIdType]] =
    Lens.lensu((rc, v) => rc.copy(deviceIdExistence = v), _.deviceIdExistence)

  val osLens: RuleConditions @> Option[RuleCondition[OsId]] =
    Lens.lensu((rc, v) => rc.copy(os = v), _.os)

  val wifiLens: RuleConditions @> Option[RuleCondition[ConnectionType]] =
    Lens.lensu((rc, v) => rc.copy(wifi = v), _.wifi)

  val ipAddressLens: RuleConditions @> Option[RuleCondition[IpAddressType]] =
    Lens.lensu((rc, v) => rc.copy(ipAddressExistence = v), _.ipAddressExistence)

  val appLens: RuleConditions @> Option[RuleCondition[AppId]] =
    Lens.lensu((rc, v) => rc.copy(appAndSite = v), _.appAndSite)

  val bannerLens: RuleConditions @> Option[RuleCondition[BannerType]] =
    Lens.lensu((rc, v) => rc.copy(banner = v), _.banner)

  val mraidLens: RuleConditions @> Option[RuleCondition[MraidType]] =
    Lens.lensu((rc, v) => rc.copy(mraid = v), _.mraid)

  val interstitialLens: RuleConditions @> Option[RuleCondition[InterstitialType]] =
    Lens.lensu((rc, v) => rc.copy(interstitial = v), _.interstitial)

  val videoLens: RuleConditions @> Option[RuleCondition[VideoType]] =
    Lens.lensu((rc, v) => rc.copy(video = v), _.video)

  val nativeLens: RuleConditions @> Option[RuleCondition[NativeType]] =
    Lens.lensu((rc, v) => rc.copy(native = v), _.native)

  val countryLens: RuleConditions @> Option[RuleCondition[CountryIdAlpha2]] =
    Lens.lensu((rc, v) => rc.copy(country = v), _.country)

  val latLongLens: RuleConditions @> Option[RuleCondition[LatLongType]] =
    Lens.lensu((rc, v) => rc.copy(latlong = v), _.latlong)

  val adSizeLens: RuleConditions @> Option[RuleCondition[AdSizeId]] =
    Lens.lensu((rc, v) => rc.copy(adSize = v), _.adSize)

  val regionLens: RuleConditions @> Option[RuleCondition[RegionId]] =
    Lens.lensu((rc, v) => rc.copy(region = v), _.region)

  val cityLens: RuleConditions @> Option[RuleCondition[CityName]] =
    Lens.lensu((rc, v) => rc.copy(city = v), _.city)

  val zipLens: RuleConditions @> Option[RuleCondition[ZipAlpha]] =
    Lens.lensu((rc, v) => rc.copy(zip = v), _.zip)

  implicit val format = Json.format[RuleConditions]

  def builder(
    carrier: Option[RuleCondition[CarrierId]] = None,
    handset: Option[RuleCondition[HandsetId]] = None,
    deviceIdExistence: Option[RuleCondition[DeviceIdType]] = None,
    os: Option[RuleCondition[OsId]] = None,
    wifi: Option[RuleCondition[ConnectionType]] = None,
    ipAddressExistence: Option[RuleCondition[IpAddressType]] = None,
    appAndSite: Option[RuleCondition[AppId]] = None,
    banner: Option[RuleCondition[BannerType]] = None,
    mraid: Option[RuleCondition[MraidType]] = None,
    interstitial: Option[RuleCondition[InterstitialType]] = None,
    video: Option[RuleCondition[VideoType]] = None,
    native: Option[RuleCondition[NativeType]] = None,
    adSize: Option[RuleCondition[AdSizeId]] = None,
    latlong: Option[RuleCondition[LatLongType]] = None,
    country: Option[RuleCondition[CountryIdAlpha2]] = None,
    region: Option[RuleCondition[RegionId]] = None,
    city: Option[RuleCondition[CityName]] = None,
    zip: Option[RuleCondition[ZipAlpha]] = None,
    userList: Option[RuleCondition[UserListName]] = None): RuleConditions =
    RuleConditions(carrier, handset, deviceIdExistence, os, wifi, ipAddressExistence,
      appAndSite, banner, mraid, interstitial, video, native, adSize, latlong,
      country, region, city, zip, userList)

}

// Do we want to Split Rule into a Trait and then have a
// MobileSupplyRule and a WebSupplyRule to abstract the different types
// that go with exchanges (and the different attributes for each)?
case class Rule(
  id: RuleId,
  name: RuleName,
  created: RuleCreatedInstant,
  trafficType: TrafficType,
  conditions: RuleConditions)
object Rule extends EqualzAndShowz[Rule] {
  val idLens: Rule @> RuleId =
    Lens.lensu((r, v) => r.copy(id = v), _.id)
  val nameLens: Rule @> RuleName =
    Lens.lensu((r, v) => r.copy(name = v), _.name)
  val conditionsLens: Rule @> RuleConditions =
    Lens.lensu((r, v) => r.copy(conditions = v), _.conditions)
  val createdLens: Rule @> RuleCreatedInstant =
    Lens.lensu((r, v) => r.copy(created = v), _.created)
  val createdValueLens: Rule @> Instant =
    createdLens >=> RuleCreatedInstant.valueLens
  val trafficTypeLens: Rule @> TrafficType =
    Lens.lensu((rc, v) => rc.copy(trafficType = v), _.trafficType)


  implicit val format = Json.format[Rule]

  def newRule(
    id: RuleId,
    name: RuleName,
    created: RuleCreatedInstant,
    trafficType: TrafficType): Rule =
    Rule(
      id,
      name,
      created,
      trafficType,
      RuleConditions.Empty)
}


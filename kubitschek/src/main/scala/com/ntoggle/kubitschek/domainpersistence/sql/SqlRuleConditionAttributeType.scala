package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.users.UserListId
import com.ntoggle.kubitschek.domain._
import scalaz.std.string._
import scalaz.syntax.equal._

private[sql] case class SqlRuleConditionAttributeType(value: String)
private[sql] sealed trait ConditionType[A] {
  def key: String
  def conditionType: SqlRuleConditionAttributeType =
    SqlRuleConditionAttributeType(key)
  def unapply(
    a: SqlRuleConditionAttributeType): Option[SqlRuleConditionAttributeType] =
    if (a.value === key) Some(a) else None
}
private[sql] object ConditionType {

  implicit object TrafficType extends ConditionType[TrafficType] {
    lazy val key = "trafficType"
  }
  implicit object Carrier extends ConditionType[CarrierId] {
    lazy val key = "carrier"
  }
  implicit object Handset extends ConditionType[HandsetId] {
    lazy val key = "handset"
  }
  implicit object DeviceIdType extends ConditionType[DeviceIdType] {
    lazy val key = "deviceIdType"
  }
  implicit object OS extends ConditionType[OsId] {
    lazy val key = "os"
  }
  implicit object ConnectionType extends ConditionType[ConnectionType] {
    lazy val key = "connectionType"
  }
  implicit object IpAddressType extends ConditionType[IpAddressType] {
    lazy val key = "ipAddressType"
  }
  implicit object App extends ConditionType[AppId] {
    lazy val key = "app"
  }
  implicit object BannerType extends ConditionType[BannerType] {
    lazy val key = "bannerType"
  }
  implicit object MraidType extends ConditionType[MraidType] {
    lazy val key = "mraidType"
  }
  implicit object InterstitialType extends ConditionType[InterstitialType] {
    lazy val key = "interstitialType"
  }
  implicit object VideoType extends ConditionType[VideoType] {
    lazy val key = "videoType"
  }
  implicit object NativeType extends ConditionType[NativeType] {
    lazy val key = "nativeType"
  }
  implicit object AdSize extends ConditionType[AdSizeId] {
    lazy val key = "adSize"
  }
  implicit object LatLong extends ConditionType[LatLongType] {
    lazy val key = "latlong"
  }
  implicit object Country extends ConditionType[CountryIdAlpha2] {
    lazy val key = "country"
  }
  implicit object Region extends ConditionType[RegionId] {
    lazy val key = "region"
  }
  implicit object City extends ConditionType[CityName] {
    lazy val key = "city"
  }
  implicit object Zip extends ConditionType[ZipAlpha] {
    lazy val key = "zip"
  }
  implicit object UserList extends ConditionType[UserListName] {
    lazy val key = "userList"
  }
}
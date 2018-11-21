package com.ntoggle.kubitschek
package domain

import com.ntoggle.goldengate.playjson.PlayJsonUtils
import com.ntoggle.goldengate.playjson.PlayJsonSyntax._
import com.ntoggle.goldengate.playjson.PlayFormat._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.syntax.std.option._

sealed trait AttributeType
object AttributeType {
  object Keys {
    val AppAttr = "App"
    val CountryAttr = "Country"
    val HandsetAttr = "Handset"
    val RegionAttr = "Region"
    val CityAttr = "City"
    val OsAttr = "OS"
    val CarrierAttr = "Carrier"
    val UserListAttr = "UserList"

    val All = Seq(AppAttr, CountryAttr, HandsetAttr, RegionAttr, CityAttr, OsAttr, CarrierAttr, UserListAttr)
  }
  def fromStringKey(s: String): Option[AttributeType] = s match {
    case Keys.AppAttr => Some(AppAttr)
    case Keys.CountryAttr => Some(CountryAttr)
    case Keys.HandsetAttr => Some(HandsetAttr)
    case Keys.RegionAttr => Some(RegionAttr)
    case Keys.CityAttr => Some(CityAttr)
    case Keys.OsAttr => Some(OsAttr)
    case Keys.CarrierAttr => Some(CarrierAttr)
    case Keys.UserListAttr => Some(UserListAttr)
    case _ => None
  }

  def toStringKey(a: AttributeType): String = a match {
    case AppAttr => Keys.AppAttr
    case CountryAttr =>Keys.CountryAttr
    case HandsetAttr =>Keys.HandsetAttr
    case RegionAttr => Keys.RegionAttr
    case CityAttr => Keys.CityAttr
    case OsAttr => Keys.OsAttr
    case CarrierAttr => Keys.CarrierAttr
    case UserListAttr => Keys.UserListAttr
  }
  implicit val formatAttributeType: Format[AttributeType] = {
    val reads = Reads {
      case JsString(s) => fromStringKey(s).\/>("Invalid Attribute Type").toJsResult
      case other => PlayJsonUtils.jsError("Attribute Type must be string")
    }
    val writes: Writes[AttributeType] =
      implicitly[Writes[String]].contramap((s: AttributeType) => toStringKey(s))
    Format(reads, writes)
  }
}
object AppAttr extends AttributeType
object CountryAttr extends AttributeType
object HandsetAttr extends AttributeType
object RegionAttr extends AttributeType
object CityAttr extends AttributeType
object OsAttr extends AttributeType
object CarrierAttr extends AttributeType
object UserListAttr extends AttributeType


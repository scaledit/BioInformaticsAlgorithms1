package com.ntoggle.kubitschek
package application

import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType
import com.ntoggle.goldengate.elasticsearch.Size
import com.ntoggle.humber.catalog.CatalogApi.{SuggestOutputText, SuggestRequestString}
import com.ntoggle.humber.catalog.ESCatalog.AutoCompleteError
import com.ntoggle.kubitschek.catalog.SuggestId
import com.ntoggle.kubitschek.domain.AttributeType

import scala.concurrent.Future
import scalaz.syntax.std.option._

class FeatureIndexStub(private var features: Map[String, Map[String, String]]) {

  val autoComplete = (
    dpId: DemandPartnerId,
    spId: SupplyPartnerId,
    trafficType: TrafficType,
    size: Size,
    attr: AttributeType,
    q: SuggestRequestString) => {
    val result = features.get(AttributeType.toStringKey(attr)).map(_.filter {
      case (k, v) => k.toLowerCase.startsWith(q.value.toLowerCase) ||
        v.toLowerCase.startsWith(q.value.toLowerCase)}
      .map(t => SuggestId(t._1) -> SuggestOutputText(t._2))
      .toList.take(size.value))
      .\/>(AutoCompleteError("Something went wrong with index stub"))
    Future.successful(result)
  }
}
object FeatureIndexStub {
  val OS1 = OsId(OsName("iOS"), OsVersion("9.0"))
  val OS2 = OsId(OsName("iOS"), OsVersion("8.3"))
  val OS3 = OsId(OsName("iOS"), OsVersion("9.0.1"))
  val HS1 = HandsetId(HandsetManufacturer("LGE"), HandsetModel("LG-D605"))
  val HS2 = HandsetId(HandsetManufacturer("Apple"), HandsetModel("iPad mini 2G (Cellular)"))
  val HS3 = HandsetId(HandsetManufacturer("Apple"), HandsetModel("iPhone 5 (GSM+CDMA)"))

  def stubData = new FeatureIndexStub(Map(
    "OS" -> Map(
      OsId.toStringKey(OS1) -> (OS1.name.value + " " + OS1.version.version),
      OsId.toStringKey(OS2) -> (OS2.name.value + " " + OS2.version.version),
      OsId.toStringKey(OS3) -> (OS3.name.value + " " + OS3.version.version)),
    "Carrier" -> Map(
      "344-92" -> "AT&T Wireless 344-92",
      "310-004" -> "Verizon Wireless 310-004",
      "310-016" -> "Cricket Wireless 310-016"),
    "Region" -> Map("NY" -> "New York", "MA" -> "Massachusetts", "CA" -> "California"),
    "City" -> Map("New York City" -> "New York City", "Boston" -> "Boston", "Bangkok" -> "Bangkok"),
    "App" -> Map(
      "agltb3B1Yi1pbmNyDAsSA0FwcBiiw_cSDA" -> "jango radio",
      "agltb3B1Yi1pbmNyDAsSA0FwcBiKt5IUDA" -> "Alarm Clock +",
      "f7cc119ca9e1426c8d162d2d37c8558f" -> "Android Skout New"),
    "Handset" -> Map(
      HandsetId.toStringKey(HS1) -> s"${HS1.manufacturer.value} ${HS1.handset.value}",
      HandsetId.toStringKey(HS2) -> s"${HS2.manufacturer.value} ${HS2.handset.value}",
      HandsetId.toStringKey(HS3) -> s"${HS3.manufacturer.value} ${HS3.handset.value}"),
    "Country" -> Map("TH" -> "Thailand", "US" -> "United States of America", "BR" -> "Brazil")))
}
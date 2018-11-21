package com.ntoggle.kubitschek.application

import com.ntoggle.goldengate.elasticsearch.IndexName
import com.ntoggle.humber.catalog._
import com.ntoggle.kubitschek.domain._
import com.typesafe.config.Config

object FeatureIndexConfig {
  def fromConfig(config: Config): FeatureIndexConfig =
    FeatureIndexConfig(
      AppReadIndexName(IndexName(config.getString("apps-index"))),
      HandsetReadIndexName(IndexName(config.getString("handsets-index"))),
      OsReadIndexName(IndexName(config.getString("os-index"))),
      CityReadIndexName(IndexName(config.getString("city-index"))),
      CarrierReadIndexName(IndexName(config.getString("carrier-index"))))
}
case class FeatureIndexConfig(
  appIndex: AppReadIndexName,
  handsetIndex: HandsetReadIndexName,
  osIndex: OsReadIndexName,
  cityIndex: CityReadIndexName,
  carrierIndex: CarrierReadIndexName) {
  def fromAttributeType(attr: AttributeType): FeatureSpecificIndex = attr match {
    case AppAttr => appIndex
    case HandsetAttr => handsetIndex
    case OsAttr => osIndex
    case CityAttr => cityIndex
    case CarrierAttr => carrierIndex
    case _ => throw new Exception("Feature Error: This index should not use ES")
  }
}

package com.ntoggle.kubitschek.domain

import com.ntoggle.albi.EstimatedAvailableQps
import play.api.libs.json._

case class MetricCount(
  oneHour: Option[Long],
  oneDay: Option[Long],
  sevenDays: Option[Long])
object MetricCount {

  implicit val formatMetricCount: Format[MetricCount] =
    Json.format[MetricCount]

  val Empty = MetricCount(None, None, None)
}


case class EndpointAvailableForecast(
  oneDay: Option[EstimatedAvailableQps])
object EndpointAvailableForecast {

  implicit val formatEndpointAvailableForecast: Format[EndpointAvailableForecast] =
    Json.format[EndpointAvailableForecast]
}

case class RuleAvailableForecast(
  oneDay: Option[EstimatedAvailableQps])
object RuleAvailableForecast {

  implicit val formatRuleAvailableForecast: Format[RuleAvailableForecast] =
    Json.format[RuleAvailableForecast]
}

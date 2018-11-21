package com.ntoggle.kubitschek
package application

import com.ntoggle.goldengate.elasticsearch.IndexName
import com.ntoggle.goldengate.jodaext.DateTimes
import com.ntoggle.humber.estimation.{SecondsCountsReadIndexName, BidRequestReadIndexName, EstimationDuration}
import com.typesafe.config.Config
import org.joda.time.{Period, Duration}

object EstimationConfig {
  def fromConfig(config: Config): EstimationConfig = {
    EstimationConfig(
      EstimationDuration(
        Period.parse(config.getString("duration")).toDurationTo(DateTimes.utcNow())),
      BidRequestReadIndexName(IndexName(config.getString("bid-request-index"))),
      SecondsCountsReadIndexName(IndexName(config.getString("second-index"))))
  }
}
case class EstimationConfig(
  estimationDuration: EstimationDuration,
  bidRequestIndex: BidRequestReadIndexName,
  secondsIndex: SecondsCountsReadIndexName)

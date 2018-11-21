package com.ntoggle.kubitschek.services

import com.ntoggle.audobon.{Series, MinuteTimestampUTC, TimeRange}
import com.ntoggle.kubitschek.application.HttpServiceConfig
import com.ntoggle.kubitschek.domain._
import com.ntoggle.audobon.web.client.WebClient
import org.joda.time.{DateTime, Duration}

import scala.concurrent.{ExecutionContext, Future}
import com.ntoggle.audobon.{RuleId => AudobonRuleId}


class MetricsService(config: HttpServiceConfig, ec: ExecutionContext) {

  private val client: WebClient =
    new WebClient(config.interface, config.port)(ec)
  // could make this a config, for the moment this is fine
  private final val errorDelta = Duration.standardMinutes(1)
  private final val oneHour = Duration.standardHours(1)
  private final val oneDay = Duration.standardDays(1)
  private final val oneWeek = Duration.standardDays(7)


  private def timeUntil(duration: Duration): TimeRange = {
    TimeRange(
      MinuteTimestampUTC.fromDateTime(DateTime.now().minus(errorDelta)
        .minus(duration)),
      MinuteTimestampUTC.fromDateTime(DateTime.now().minus(errorDelta)))
  }

  private def countSeries(series: Series): Long =
    series.points.foldLeft(0L) { (total, n) => total + n.value }

  private def toQps(count: Long, duration: Duration): Long =
    Math.ceil(count / duration.getStandardSeconds).toLong

  private implicit val iec = ec

  def requests: RouterConfigurationId => Future[MetricCount] =
    id => for {
      hour <- client.transmittedTotal(id.spId, id.dpId, timeUntil(oneHour))
      hourlyQps = toQps(countSeries(hour.series), oneHour)
      day <- client.transmittedTotal(id.spId, id.dpId, timeUntil(oneDay))
      dailyQps = toQps(countSeries(day.series), oneDay)
      week <- client.transmittedTotal(id.spId, id.dpId, timeUntil(oneWeek))
      weeklyQps = toQps(countSeries(week.series), oneWeek)
    } yield
      MetricCount(Some(hourlyQps), Some(dailyQps), Some(weeklyQps))


  def bids: RouterConfigurationId => Future[MetricCount] =
    id => for {
      hour <- client.bidsReceived(id.spId, id.dpId, timeUntil(oneHour))
      hourlyQps = toQps(countSeries(hour.series), oneHour)
      day <- client.bidsReceived(id.spId, id.dpId, timeUntil(oneDay))
      dailyQps = toQps(countSeries(day.series), oneDay)
      week <- client.bidsReceived(id.spId, id.dpId, timeUntil(oneWeek))
      weeklyQps = toQps(countSeries(week.series), oneWeek)
    } yield
      MetricCount(Some(hourlyQps), Some(dailyQps), Some(weeklyQps))

  def ruleActuals: (RouterConfigurationId, RuleId) => Future[MetricCount] =
    (rc, r) => for {
      hour <- client.transmittedForRule(
        rc.spId, rc.dpId, AudobonRuleId(r.value), timeUntil(oneHour))
      hourlyQps = toQps(countSeries(hour.series), oneHour)
      day <- client.transmittedForRule(
        rc.spId, rc.dpId, AudobonRuleId(r.value), timeUntil(oneDay))
      dailyQps = toQps(countSeries(day.series), oneDay)
      week <- client.transmittedForRule(
        rc.spId, rc.dpId, AudobonRuleId(r.value), timeUntil(oneWeek))
      weeklyQps = toQps(countSeries(week.series), oneWeek)
    } yield
      MetricCount(Some(hourlyQps), Some(dailyQps), Some(weeklyQps))
}

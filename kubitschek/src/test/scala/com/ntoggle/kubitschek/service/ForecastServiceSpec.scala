package com.ntoggle.kubitschek
package service

import com.ntoggle.albi.{Mobile, SupplyPartnerId, DemandPartnerId, EstimatedAvailableQps}
import com.ntoggle.kubitschek.api.GetForecastResponse
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.services.ForecastService
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scalaz.\/-

class ForecastServiceSpec extends Specification{
  "ForecastService" should {
    val cond = RuleConditions(
      Some(RuleCondition(AllowAllDefaultConditionAction, Set(), AllowUndefinedConditionAction)),
      None, None, None, None, None, None, None, None, None,
      None, None, None, None, None, None, None, None, None)
    "Get Forecast correctly" ! {
      val expectedQPS = EstimatedAvailableQps(10000)
      def getForecast = ForecastService.get(
        (_, _, _, rc: RuleConditions) => Future.successful(expectedQPS)
      )
      val fe = Await.result(getForecast(
        SupplyPartnerId("1234"),
        DemandPartnerId("5678"),
        Mobile, cond).run, 1000 milli)
      fe ==== \/-(GetForecastResponse(expectedQPS))
    }
  }
}

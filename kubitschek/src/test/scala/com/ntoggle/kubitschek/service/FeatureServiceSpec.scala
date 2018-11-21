package com.ntoggle.kubitschek.service

import com.ntoggle.albi.{TrafficType, Mobile, SupplyPartnerId, DemandPartnerId}
import com.ntoggle.goldengate.elasticsearch.Size
import com.ntoggle.humber.catalog.CatalogApi.{SuggestOutputText, SuggestRequestString}
import com.ntoggle.humber.catalog.ESCatalog.AutoCompleteError
import com.ntoggle.kubitschek.api.{FeaturePair, GetFeatureResponse, GetFeatureParamRequest}
import com.ntoggle.kubitschek.catalog.SuggestId
import com.ntoggle.kubitschek.domain.{AttributeType, AppAttr}
import com.ntoggle.kubitschek.infra.BadRequestRejection
import com.ntoggle.kubitschek.services.FeatureService
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scalaz.{-\/, \/-}

class FeatureServiceSpec extends Specification {
  val expectedDpId = DemandPartnerId("dp1")
  val expectedSpId = SupplyPartnerId("sp1")
  val expectedTrafficType = Mobile
  val expectedSize = Size(10)
  val expectedAttr = AppAttr
  val requestString = SuggestRequestString("Req")
  "FeatureService" should {
    "Get suggestions correctly" ! {
      val expectedResult = List((SuggestId("payload"), SuggestOutputText("output")))
      def getFeatureSuggest = FeatureService.get(
        (dpId: DemandPartnerId,
          spId: SupplyPartnerId,
          trafficType: TrafficType,
          size: Size,
          attr: AttributeType,
          q: SuggestRequestString) => Future.successful(\/-(expectedResult)))
      val fe = Await.result(getFeatureSuggest(GetFeatureParamRequest(
        expectedDpId,
        expectedSpId,
        expectedTrafficType,
        expectedSize,
        expectedAttr,
        requestString)).run, 1000 milli)
      fe ==== \/-(GetFeatureResponse(expectedResult.map(r => FeaturePair(r._1, r._2))))
    }
    "Fail when ES fails" ! {
      def getFeatureSuggest = FeatureService.get(
        (dpId: DemandPartnerId,
          spId: SupplyPartnerId,
          trafficType: TrafficType,
          size: Size,
          attr: AttributeType,
          q: SuggestRequestString) => Future.successful(-\/(AutoCompleteError("TESTERROR"))))
      val fe = Await.result(getFeatureSuggest(GetFeatureParamRequest(
        expectedDpId,
        expectedSpId,
        expectedTrafficType,
        expectedSize,
        expectedAttr,
        requestString)).run, 1000 milli)
      fe ==== -\/(BadRequestRejection(
        "error attempting to perform action",
        Some(Json.toJson("TESTERROR"))))
    }
  }
}

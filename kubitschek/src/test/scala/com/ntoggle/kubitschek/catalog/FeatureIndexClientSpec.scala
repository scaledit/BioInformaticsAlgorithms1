package com.ntoggle.kubitschek
package catalog

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.elasticsearch.{IndexName, Size}
import com.ntoggle.humber.catalog._
import com.ntoggle.humber.catalog.CatalogApi.{SuggestOutputText, SuggestRequestString}
import com.ntoggle.kubitschek.domain._
import org.joda.time.Instant
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scalaz.\/-

class FeatureIndexClientSpec extends Specification {
  "FeatureIndexClient" should {
    "autocomplete correctly" ! {
      val dpId = DemandPartnerId("dp1")
      val spId = SupplyPartnerId("sp1")
      val trafficType = Mobile
      val size = Size(10)
      val attr = AppAttr
      val requestString = SuggestRequestString("Req")

      val AppIndex = AppReadIndexName(IndexName("AppIndex"))
      val HandsetIndex = HandsetReadIndexName(IndexName("HandsetIndex"))
      val CityIndex = CityReadIndexName(IndexName("CityIndex"))
      val OsIndex = OsReadIndexName(IndexName("OSIndex"))
      val CarrierIndex = CarrierReadIndexName(IndexName("CarrierIndex"))
      val getUserLists = (dpId: Option[DemandPartnerId], o: Offset, l: Limit) =>
        Future.successful(List(UserList(UserListId(""), UserListName(""), DemandPartnerId(""), CreatedInstant(new Instant(1000)), ModifiedInstant(new Instant(1000)))))

      val appAC = (q: SuggestRequestString, s: Size, sp: SupplyPartnerId, dt: TrafficType) =>
        Future.successful(\/-(List(AppId("App") -> SuggestOutputText(AppIndex.forSupplyPartnerAndTraffic(sp, dt).name))))
      val handsetAC = (q: SuggestRequestString, s: Size, sp: SupplyPartnerId, dt: TrafficType) =>
        Future.successful(\/-(List(HandsetId(HandsetManufacturer("HMake"),
          HandsetModel("HModel")) -> SuggestOutputText(HandsetIndex.forSupplyPartnerAndTraffic(sp, dt).name))))
      val osAC = (q: SuggestRequestString, s: Size, sp: SupplyPartnerId, dt: TrafficType) =>
        Future.successful(\/-(List(OsId(OsName("OsName"), OsVersion("OsVersion")) -> SuggestOutputText(OsIndex.forSupplyPartnerAndTraffic(sp, dt).name))))
      val cityAC = (q: SuggestRequestString, s: Size, sp: SupplyPartnerId, dt: TrafficType) =>
        Future.successful(\/-(List(City(CityName("City")) -> SuggestOutputText(CityIndex.forSupplyPartnerAndTraffic(sp, dt).name))))
      val carrierAC = (q: SuggestRequestString, s: Size, sp: SupplyPartnerId, dt: TrafficType) =>
        Future.successful(\/-(List(CarrierId("Carrier") -> SuggestOutputText(CarrierIndex.forSupplyPartnerAndTraffic(sp, dt).name))))

      val featureClient = new FeatureIndexClient(getUserLists, appAC, handsetAC, osAC, cityAC, carrierAC)
      
      val resultApps = Await.result(featureClient.autoComplete(
        dpId, spId, trafficType, size, AppAttr, requestString), 1000 milli)
      val expectedApps =
        \/-(List(SuggestId("App") -> SuggestOutputText(s"${AppIndex.forSupplyPartnerAndTraffic(spId, trafficType).name}")))
      resultApps ==== expectedApps

      val resultHandset = Await.result(featureClient.autoComplete(
        dpId, spId, trafficType, size, HandsetAttr, requestString), 1000 milli)
      val expectedHandset =
        \/-(List(SuggestId("F484D616B65-F484D6F64656C") -> SuggestOutputText(s"${HandsetIndex.forSupplyPartnerAndTraffic(spId, trafficType).name}")))
      resultHandset ==== expectedHandset

      val resultOS = Await.result(featureClient.autoComplete(
        dpId, spId, trafficType, size, OsAttr, requestString), 1000 milli)
      val expectedOS =  
        \/-(List(SuggestId("F4F734E616D65-F4F7356657273696F6E") -> SuggestOutputText(s"${OsIndex.forSupplyPartnerAndTraffic(spId, trafficType).name}")))
      resultOS ==== expectedOS

      val resultCity = Await.result(featureClient.autoComplete(
        dpId, spId, trafficType, size, CityAttr, requestString), 1000 milli)
      val expectedCity =  
        \/-(List(SuggestId("City") -> SuggestOutputText(s"${CityIndex.forSupplyPartnerAndTraffic(spId, trafficType).name}")))
      resultCity ==== expectedCity

      val resultCarrier = Await.result(featureClient.autoComplete(
        dpId, spId, trafficType, size, CarrierAttr, requestString), 1000 milli)
      val expectedCarrier =  
        \/-(List(SuggestId("Carrier") -> SuggestOutputText(s"${CarrierIndex.forSupplyPartnerAndTraffic(spId, trafficType).name}")))
      resultCarrier ==== expectedCarrier
    }
  }
}

package com.ntoggle.kubitschek
package service

import akka.http.scaladsl.server.Rejection
import com.ntoggle.albi._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.services.{SupplyPartnerService, DemandPartnerService}
import com.ntoggle.kubitschek.infra.{BadRequestRejection, ValidatedDirectives}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz._
import scalaz.syntax.apply._
import scalaz.syntax.std.option._

class PartnerServiceSpec extends Specification {

  "DemandPartnerService" should {
    val expectedId = "dpId1"
    val expecteddpn = DemandPartnerName("dp1")
    "create demand partner returns the correct demand partner" ! {

      def createDemandPartner = DemandPartnerService.create(
        () => Future.successful(expectedId),
        (d: DemandPartner) => Future.successful(
          Some(d).\/>(ConfigurationError.generalError("ERROR"))))

      val fe = Await.result(createDemandPartner(expecteddpn).run, 1000.milli)
      fe ==== \/-(DemandPartner(DemandPartnerId(expectedId), expecteddpn))
    }

    "create demand partner with existing id fails" ! {

      def createDemandPartner = DemandPartnerService.create(
        () => Future.successful(expectedId),
        (d: DemandPartner) => Future.successful(
          -\/(ConfigurationError.demandPartnerAlreadyExists(DemandPartnerId(expectedId)))))

      val fe = Await.result(createDemandPartner(expecteddpn).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("demand partner already exists", Some(Json.toJson(DemandPartnerId(expectedId)))))
    }

    "create demand partner with existing name fails" ! {

      def createDemandPartner = DemandPartnerService.create(
        () => Future.successful(expectedId),
        (d: DemandPartner) => Future.successful(
          -\/(ConfigurationError.demandPartnerNameAlreadyExists(expecteddpn))))

      val fe = Await.result(createDemandPartner(expecteddpn).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("demand partner name already exists", Some(Json.toJson(expecteddpn))))
    }
  }

  "SupplyPartnerService" should {
    val expectedId = "spId1"
    val expectedspn = SupplyPartnerName("sp1")

    "create supply partner returns the correct supply partner" ! {

      def createSupplyPartner = SupplyPartnerService.create(
        () => Future.successful(expectedId),
        (tsp: SupplyPartner) => Future.successful(
          Some(tsp).\/>(ConfigurationError.generalError("ERROR"))))

      val fe = Await.result(createSupplyPartner(expectedspn).run, 1000.milli)
      fe ==== \/-(SupplyPartner(SupplyPartnerId(expectedId), expectedspn))
    }

    "create supply partner with existing id fails" ! {

      def createSupplyPartner = SupplyPartnerService.create(
        () => Future.successful(expectedId),
        (tsp: SupplyPartner) => Future.successful(
          -\/(ConfigurationError.supplyPartnerAlreadyExists(SupplyPartnerId(expectedId)))))

      val fe = Await.result(createSupplyPartner(expectedspn).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("supply partner already exists", Some(Json.toJson(SupplyPartnerId(expectedId)))))
    }

    "create supply partner with existing name fails" ! {

      def createSupplyPartner = SupplyPartnerService.create(
        () => Future.successful(expectedId),
        (d: SupplyPartner) => Future.successful(
          -\/(ConfigurationError.supplyPartnerNameAlreadyExists(expectedspn))))

      val fe = Await.result(createSupplyPartner(expectedspn).run, 1000.milli)
      fe ==== -\/(BadRequestRejection("supply partner name already exists", Some(Json.toJson(expectedspn))))
    }
  }
}

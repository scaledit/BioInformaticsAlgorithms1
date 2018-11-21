package com.ntoggle.kubitschek
package service

import com.ntoggle.albi._
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.infra.BadRequestRejection
import com.ntoggle.kubitschek.services.ConfigurationService
import org.joda.time.Instant
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.{\/-, -\/}
import scalaz.syntax.std.option._

class ConfigurationServiceSpec extends Specification {

  "ConfigurationService" should {
    val edpId = DemandPartnerId("dpId1")
    val espId = SupplyPartnerId("spId1")
    val emqps = DemandPartnerMaxQps(MaxQps(10000))
    val nraddress = ConfigurationEndpoint("127.0.0.1", Port(8281))
    val createdInstant = CreatedInstant(new Instant(123456))
    val evId = VersionId("vId")
    val testReq = RouterConfigurationRequest(nraddress, emqps)
    val ercId = RouterConfigurationId(edpId, espId)

    "create Router Configuration" ! {

      def createRouterConfiguration = ConfigurationService.createRouterConfiguration(
        () => Future.successful(evId.value),
        () => Future.successful(createdInstant.value),
        (rc, vId, ci, mqps) => Future.successful(
          Some(rc).\/>(ConfigurationError.generalError("ERROR"))))

      val fe = Await.result(createRouterConfiguration(ercId, testReq).run, 1000.milli)
      val expectedResponse = CreateRouterConfigurationResponse(evId, edpId, espId, emqps, nraddress)

      fe ==== \/-(expectedResponse)
    }

    "fails when demand partner not found" ! {

      def createRouterConfiguration = ConfigurationService.createRouterConfiguration(
        () => Future.successful(espId.id),
        () => Future.successful(createdInstant.value),
        (rc, vId, ci, mqps) => Future.successful(
          -\/(ConfigurationError.demandPartnerNotFound(edpId))))

      val fe = Await.result(createRouterConfiguration(ercId, testReq).run, 1000.milli)

      fe ==== -\/(BadRequestRejection("demand partner does not exist", Some(Json.toJson(edpId))))
    }

    "fails when supply partner not found" ! {

      def createRouterConfiguration = ConfigurationService.createRouterConfiguration(
        () => Future.successful(espId.id),
        () => Future.successful(createdInstant.value),
        (rc, vId, ci, mqps) => Future.successful(
          -\/(ConfigurationError.supplyPartnerNotFound(espId))))

      val fe = Await.result(createRouterConfiguration(ercId, testReq).run, 1000.milli)

      fe ==== -\/(BadRequestRejection("supply partner does not exist", Some(Json.toJson(espId))))
    }

  }
}

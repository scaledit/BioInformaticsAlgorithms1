package com.ntoggle.kubitschek.routes

import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.{RouteTestTimeout, RouteTest}
import akka.http.specs2.Specs2Interface
import com.ntoggle.albi._
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.domainpersistence.mem.MemPersistence
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.infra.PlayJsonSupportExt
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.services.RuleService
import org.joda.time.Instant
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scalaz.{\/, \/-}

class RuleRoutesSpec   extends Specification
with RouteTest
with Specs2Interface
with ScalaCheck {

  import PlayJsonSupportExt._

  sequential

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(FiniteDuration(5, TimeUnit.SECONDS))

  val newId = () => UUID.randomUUID().toString
  val persistence = MemPersistence.empty

  val createdAt = CreatedInstant(Instant.now())
  val modifiedAt = ModifiedInstant(createdAt.value)

  val ruleCreatedAt = RuleCreatedInstant(createdAt.value)
  val dpMaxQps = DemandPartnerMaxQps(MaxQps(100))

  val user = AuthenticatedUser(
    Username("bob"),
    EmailAddress("12345@test.com"),
    Fullname("bob here"),
    UserPreferences.Default,
    Organization(DemandPartnerOrganizationId(DemandPartnerId("demand")),""),
    DemandConfiguration(List.empty)
  )
  def ruleRoutes(
    getRule: (RuleId) => Future[Option[GetRuleResponse]] =
    _ => Future.failed(new Exception("get specific rule should not have been called")),
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    _ => Future.failed(new Exception("check auth should not have been called")),
    listVersionsForRule: (RuleId, DemandPartnerId, List[SupplyPartnerId], Offset, Limit) => Future[List[Version]] =
    (_,_,_,_,_) => Future.failed(new Exception("listVersionsForRule should not have been called"))
    ): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          RulesRoutes.route(
            getRule,
            checkAuthentication,
            listVersionsForRule
          )
        }
      }
    }

  "Rules Routes" should {

    "Get specified rule" in {

      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val rId = RuleId(newId())
      val rName= RuleName("validRule")
      val vId = VersionId(newId())
      val version = Version(vId, rcId, createdAt, modifiedAt, None, dpMaxQps)
      val dt = Mobile
      val expectedRule = Rule(rId,rName,ruleCreatedAt, dt, RuleConditions.Empty)

      val expectedResponse = RuleService.toGetRuleResponse(expectedRule)

      val user = getUser(rcId)

      val route = ruleRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getRule = (id) => {
          Future.successful(
            if (id === rId)
              Some(expectedResponse)
            else None)
        },
      listVersionsForRule = (rId, dpId, spId, offset, limit) => Future.successful(List(version))
      )

      Get("/rules/" + URLEncoder.encode(rId.value, "UTF-8")) ~> route ~> check {
        val result = responseAs[GetRuleResponse]
        status === OK
      }
    }

    "Fail with not found when RuleId not found" in {

      val rId = RuleId(newId())
      val rName= RuleName("validRule")
      val dt = Mobile
      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val expectedRule = Rule(rId,rName,ruleCreatedAt, dt, RuleConditions.Empty)
      val vId = VersionId(newId())

      val version = Version(vId, rcId, createdAt, modifiedAt, None, dpMaxQps)

      val expectedResponse = RuleService.toGetRuleResponse(expectedRule)

      val user = getUserBadDemandPartner(rcId)

      val route = ruleRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getRule = (id) => {
          Future.successful(None)
        }
      )

      Get("/rules/" + URLEncoder.encode(rId.value, "UTF-8")) ~> route ~> check {
        status === NotFound
      }
    }

    "Fail with UNAUTHORIZED if no Version for Rule" in {

      val rId = RuleId(newId())
      val rName= RuleName("validRule")
      val dt = Mobile
      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val expectedRule = Rule(rId,rName,ruleCreatedAt, dt, RuleConditions.Empty)
      val vId = VersionId(newId())

      val version = Version(vId, rcId, createdAt, modifiedAt, None, dpMaxQps)

      val expectedResponse = RuleService.toGetRuleResponse(expectedRule)

      val user = getUserBadDemandPartner(rcId)

      val route = ruleRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getRule = (id) => {
          Future.successful(
            if (id === rId)
              Some(expectedResponse)
            else None)
        },
        listVersionsForRule = (rId, dpId, spId, offset, limit) => Future.successful(List.empty)
      )

      Get("/rules/" + URLEncoder.encode(rId.value, "UTF-8")) ~> route ~> check {
        status === Unauthorized
      }
    }

  }

  private def getUser(rcId:RouterConfigurationId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(rcId.dpId),name="demand"),
      DemandConfiguration(List(SupplyPartner(rcId.spId,SupplyPartnerName(""))))
    )
  }

  private def getUserBadDemandPartner(rcId:RouterConfigurationId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(DemandPartnerId(newId())),name="demand"),
      DemandConfiguration(List(SupplyPartner(rcId.spId,SupplyPartnerName(""))))
    )
  }

  private def getUserBadSupplyPartner(rcId:RouterConfigurationId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(rcId.dpId),name="demand"),
      DemandConfiguration(List(SupplyPartner(SupplyPartnerId(newId()),SupplyPartnerName(""))))
    )
  }

}

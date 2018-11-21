package com.ntoggle.kubitschek.routes

import java.net.URLEncoder
import java.util.UUID

import akka.http.scaladsl.model.{HttpHeader, HttpEntity}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Rejection
import akka.http.scaladsl.testkit.RouteTest
import akka.http.specs2.Specs2Interface
import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType._
import com.ntoggle.goldengate.NGen
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.domainpersistence.mem.MemPersistence
import com.ntoggle.kubitschek.infra.ParamExtractor.ExpectedBasicTypes
import com.ntoggle.kubitschek.infra.{ParamExtractorError, PathExtractorRejection, PlayJsonSupportExt, ErrorMessage}
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.services.{AuthenticationService, RuleService}

import org.joda.time.Instant

import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.{JsString, Json}

import scala.concurrent.Future
import scalaz.{\/-, \/, -\/, EitherT}
import scalaz.std.scalaFuture._


class VersionRoutesSpec
  extends Specification
  with RouteTest
  with Specs2Interface
  with ScalaCheck {

  import PlayJsonSupportExt._
  import com.ntoggle.kubitschek.domain.DomainGenerators._

  sequential

  val newId = () => UUID.randomUUID().toString
  val persistence = MemPersistence.empty

  val createdAt = CreatedInstant(Instant.now())
  val modifiedAt = ModifiedInstant(createdAt.value)
  val ruleCreatedAt = RuleCreatedInstant(createdAt.value)

  val user = AuthenticatedUser(
    Username("bob"),
    EmailAddress("12345@test.com"),
    Fullname("bob here"),
    UserPreferences.Default,
    Organization(DemandPartnerOrganizationId(DemandPartnerId("demand")),""),
    DemandConfiguration(List.empty)
  )

  def versionRoutes(
    getVersion: (VersionId) => Future[Option[VersionSummaryResponse]] =
    _ => Future.failed(new Exception("get specific version should not have been called")),
    listVersions: (Option[DemandPartnerId], Option[SupplyPartnerId], Offset, Limit) => Future[List[Version]] =
    (dpId, spId, o, l) => Future.failed(new Exception("list meta versions should not have been called")),
    setQps: (VersionId, VersionQpsUpdateRequest) => ApiResponseFuture[VersionSummaryResponse] =
    (_,_) => EitherT.right(Future.failed(new Exception("setQps should not have been called"))),
    createVersion: (VersionCreateRequest) => ApiResponseFuture[VersionSummaryResponse] =
    _ => EitherT.right(Future.failed(new Exception("createVersion should not have been called"))),
    copyVersion: (VersionId) => ApiResponseFuture[VersionSummaryResponse] =
    _ => EitherT.right(Future.failed(new Exception("copyVersion should not have been called"))),
    publishVersion: (VersionId) => ApiResponseFuture[VersionSummaryResponse] =
    _ => EitherT.right(Future.failed(new Exception("publishVersion should not have been called"))),
    saveRule: (VersionId, CreateRuleRequest) => ApiResponseFuture[GetRuleResponse] =
    (_, _) => EitherT.right(Future.failed[GetRuleResponse](new Exception("Save rule should not have been called"))),
    replaceRule: (VersionId, RuleId, ReplaceRuleRequest) => ApiResponseFuture[GetRuleResponse] =
    (_, _, _) => EitherT.right(Future.failed[GetRuleResponse](new Exception("Update rule should not have been called"))),
    removeRule: (VersionRuleId) => ApiResponseFuture[Unit] =
    _ => EitherT.right(Future.failed[Unit](new Exception("Remove rule should not have been called"))),
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    _ => Future.failed(new Exception("check auth should not have been called"))
    ): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          VersionRoutes.route(
            getVersion,
            listVersions,
            setQps,
            createVersion,
            copyVersion,
            publishVersion,
            saveRule,
            replaceRule,
            removeRule,
            checkAuthentication
          )
        }
      }
    }

  "Version Routes" should {

    "Get specified version" in {

      val rId = RuleId(newId())
      val vId = VersionId(newId())

      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val ruleSummary = new RuleSummary(rId, Mobile, DesiredToggledQps(1000), MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName("two"))
      val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(1000)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, List(ruleSummary))

      val user = getUser(rcId)

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getVersion = (id) => {
          Future.successful(
            if (id === vId)
              Some(vs)
            else None)
        })

      Get("/versions/summary/" + URLEncoder.encode(vId.value, "UTF-8")) ~> route ~> check {
        val result = responseAs[VersionSummaryResponse]
        status === OK
      }
    }

    "Fail for wrong AuthorizedUser.demandPartner" in {

      val rId = RuleId(newId())
      val vId = VersionId(newId())

      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val ruleSummary = new RuleSummary(rId, Mobile, DesiredToggledQps(1000), MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName("two"))
      val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(1000)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, List(ruleSummary))

      val user = getUserBadDemandPartner(rcId)

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getVersion = (id) => {
          Future.successful(
            if (id === vId)
              Some(vs)
            else None)
        })

      Get("/versions/summary/" + URLEncoder.encode(vId.value, "UTF-8")) ~> route ~> check {
        status === Unauthorized
      }
    }

    "Fail for wrong AuthorizedUser.supplyPartner" in {

      val rId = RuleId(newId())
      val vId = VersionId(newId())

      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val ruleSummary = new RuleSummary(rId, Mobile, DesiredToggledQps(1000), MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName("two"))
      val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(1000)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, List(ruleSummary))

      val user = getUserBadSupplyPartner(rcId)

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getVersion = (id) => {
          Future.successful(
            if (id === vId)
              Some(vs)
            else None)
        })

      Get("/versions/summary/" + URLEncoder.encode(vId.value, "UTF-8")) ~> route ~> check {
        status === Unauthorized
      }
    }

    "list versions as array" in Prop.forAll(
      NGen.containerOfSizeRanged[List, Version](0, 2, genVersion)) { vsList =>

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        listVersions = (_, _, _, _) => Future.successful(vsList)
      )
      Get("/versions?limit=10&offset=0") ~> route ~> check {
        val result = responseAs[List[Version]]
        status === OK
      }
    }

    "Fail to list versions for invalid SP for AuthorizedUser" in Prop.forAll(

      NGen.nonEmptyListOfMaxSize[Version](2, genVersion)) { vsList =>

      // make all the spIds the same and will not match any SP on AuthorizedUser.

      val spId = SupplyPartnerId(newId())
      val dpId = DemandPartnerId(newId())

      val vsListTest = vsList.map( vs => {
        val rcId = RouterConfigurationId(dpId, spId)
        Version(vs.id, rcId, vs.created, vs.modified, vs.published, vs.maxQps)})

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        listVersions = (_, _, _, _) => Future.successful(vsListTest.list)
      )
      // request the list be filtered by SP
      Get("/versions?limit=10&offset=0&spId="+spId.id) ~> route ~> check {
        status === Unauthorized
      }
    }

    "Reject version listing without query parameters" in {
      Get("/versions") ~> versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      ) ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        status ==== NotFound
      }
    }

    "Reject version listing without limit" in {
      Get("/versions?offset=0") ~> versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      ) ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        error.cause ==== Some(JsString("limit"))
        status ==== NotFound
      }
    }

    "Reject version listing without offset" in {
      Get("/versions?limit=10") ~> versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      ) ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        error.cause ==== Some(JsString("offset"))
        status ==== NotFound
      }
    }

    "Return NotFound when versionId provided is unknown" in Prop.forAll(
      genVersionId) { id =>

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getVersion = _ =>
          Future.successful(None)
      )
      Get("/versions/summary/" + URLEncoder.encode(id.value, "UTF-8")) ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "version not found"
        status ==== NotFound
      }
    }
  }

  "Version QPS Route" should {

    "Set QPS on version" in Prop.forAll(genQpsUpdateRequest) {
      u =>

        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUser(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          setQps = (id, u) => {
            EitherT.right[Future, Rejection, VersionSummaryResponse](
              Future.successful(vs))})

        val reqBody = HttpEntity(`application/json`, Json.toJson(u).toString)

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/qps", reqBody) ~> route ~> check {
          val vsr = responseAs[VersionSummaryResponse]
          status === OK
          vsr.maxQps === u.maxQps
        }
    }

    "Fail for wrong AuthorizedUser.demandPartner" in Prop.forAll(genQpsUpdateRequest) {
      u =>

        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile,rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUserBadDemandPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          setQps = (id, u) => {
            EitherT.right[Future, Rejection, VersionSummaryResponse](
              Future.successful(vs))})

        val reqBody = HttpEntity(`application/json`, Json.toJson(u).toString)

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/qps", reqBody) ~> route ~> check {
          status === Unauthorized
        }
    }

    "Fail for wrong AuthorizedUser.supplyPartner" in Prop.forAll(genQpsUpdateRequest) {
      u =>

        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUserBadSupplyPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          setQps = (id, u) => {
            EitherT.right[Future, Rejection, VersionSummaryResponse](
              Future.successful(vs))})

        val reqBody = HttpEntity(`application/json`, Json.toJson(u).toString)

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/qps", reqBody) ~> route ~> check {
          status === Unauthorized
        }
    }

    "Reject malformed JSON" in {

      val s =
        """{
                  "maxQtps" : 10001,
                  "rules" : [
                      {
                          "id" : "7841f160-5800-11e5-8924-0002a5d5c51b",
                          "desiredQps" : 1000
                      }]
                }"""

      val cause = """{"obj.maxQps":["error.path.missing"]}"""

      val reqBody = HttpEntity(`application/json`, s)

      Post("/versions/" + VersionId(newId()).value + "/qps", reqBody) ~> versionRoutes(checkAuthentication = (_request) =>
        Future.successful(
          \/-(user))

      ) ~> check {
        status ==== BadRequest
        val error = responseAs[ErrorMessage]
        error.message ==== "JSON payload was not as expected."
        val expected = Json.parse(cause)
        error.cause ==== Some(expected)
      }
    }
  }

  "Version Rule Route" should {

    "Add a new Rule successfully" in Prop.forAll(
      genRuleId, genVersionId, genRuleName, TrafficType.genTrafficType) {
      (rId, vId, ruleName, trafficType) =>
        val conditions = RuleConditions.Empty
        val createdInstant = RuleCreatedInstant(Instant.now())
        val expectedRule = Rule(rId, ruleName, createdInstant, trafficType, conditions)
        val expectedResponse = RuleService.toGetRuleResponse(expectedRule)

        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(
          vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None,
          DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None),
          MetricCount.Empty, MetricCount.Empty, Nil)
        val user = getUser(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          saveRule = (vid, req) => EitherT.right[Future, Rejection, GetRuleResponse](
            Future.successful(GetRuleResponse(
              rId, req.name, createdInstant, trafficType,
              RuleService.toRuleConditionsResponse(req.conditions))))
        )
        val postBody = HttpEntity(`application/json`,
          Json.toJson(CreateRuleRequest(ruleName, trafficType, conditions)).toString())

        Post(s"/versions/${vId.value}/rule/add", postBody) ~> route ~> check {
          responseAs[GetRuleResponse] ==== expectedResponse
          status ==== OK
        }
    }

    "Reject request for a new Rule with invalid versionId" in {
      val rId = RuleId(newId())
      val vId = "BADID"
      val ruleName = RuleName("r1")
      val conditions = RuleConditions.Empty
      val dt = Mobile
      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      )
      val postBody = HttpEntity(`application/json`,
        Json.toJson(CreateRuleRequest(ruleName, dt, conditions)).toString())
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, vId))
      val expectedError = ErrorMessage(
        rejection.message,
        Some(rejection.cause))

      Post(s"/versions/$vId/rule/add", postBody) ~> route ~> check {
        responseAs[ErrorMessage] ==== expectedError
        status ==== BadRequest
      }
    }

    "Fail to add a Rule for wrong AuthorizedUser.demandPartner" in Prop.forAll(
      genRuleId, genVersionId, genRuleName, TrafficType.genTrafficType) {
      (rId, vId, ruleName, dt) =>
        val conditions = RuleConditions.Empty
        val createdInstant = RuleCreatedInstant(Instant.now())

        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, Nil)
        val user = getUserBadDemandPartner(rcId)
        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          saveRule = (vid, req) => EitherT.right[Future, Rejection, GetRuleResponse](
            Future.successful(GetRuleResponse(
              rId, req.name, createdInstant, dt,
              RuleService.toRuleConditionsResponse(req.conditions))))
        )
        val postBody = HttpEntity(`application/json`,
          Json.toJson(CreateRuleRequest(ruleName, dt, conditions)).toString())

        Post(s"/versions/${vId.value}/rule/add", postBody) ~> route ~> check {
          status ==== Unauthorized
        }
    }

    "Fail to add a Rule for wrong AuthorizedUser.supplyPartner" in Prop.forAll(
      genRuleId, genVersionId, genRuleName, TrafficType.genTrafficType) {
      (rId, vId, ruleName, dt) =>
        val conditions = RuleConditions.Empty
        val createdInstant = RuleCreatedInstant(Instant.now())

        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, Nil)
        val user = getUserBadSupplyPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          saveRule = (vid, req) => EitherT.right[Future, Rejection, GetRuleResponse](
            Future.successful(GetRuleResponse(
              rId, req.name, createdInstant, dt,
              RuleService.toRuleConditionsResponse(req.conditions))))
        )
        val postBody = HttpEntity(`application/json`,
          Json.toJson(CreateRuleRequest(ruleName, dt, conditions)).toString())

        Post(s"/versions/${vId.value}/rule/add", postBody) ~> route ~> check {
          status ==== Unauthorized
        }
    }

    "Replace a new Rule successfully" in Prop.forAll(
      genRuleId, genVersionId, genRuleName, TrafficType.genTrafficType) {
      (rId, vId, ruleName, dt) =>
        val conditions = RuleConditions.Empty
        val createdInstant = RuleCreatedInstant(Instant.now())
        val expectedRule = Rule(rId, ruleName, createdInstant, dt, conditions)
        val expectedResponse = RuleService.toGetRuleResponse(expectedRule)

        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, Nil)

        val user = getUser(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          replaceRule = (vid, rid, req) => EitherT.right[Future, Rejection, GetRuleResponse](
            Future.successful(GetRuleResponse(
              rId, req.name, createdInstant, dt,
              RuleService.toRuleConditionsResponse(req.conditions))))
        )
        val postBody = HttpEntity(`application/json`,
          Json.toJson(ReplaceRuleRequest(ruleName, dt, conditions)).toString())

        Post(s"/versions/${vId.value}/rule/${rId.value}/replace", postBody) ~> route ~> check {
          responseAs[GetRuleResponse] ==== expectedResponse
          status ==== OK
        }
    }

    "Reject request for replacing a Rule with invalid versionId" in {
      val vId = "BADID"
      val rId = newId()
      val ruleName = RuleName("r1")
      val conditions = RuleConditions.Empty
      val trafficType = Mobile
      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      )
      val postBody = HttpEntity(`application/json`,
        Json.toJson(ReplaceRuleRequest(ruleName, trafficType, conditions)).toString())
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, vId))
      val expectedError = ErrorMessage(
        rejection.message,
        Some(rejection.cause))

      Post(s"/versions/$vId/rule/$rId/replace", postBody) ~> route ~> check {
        responseAs[ErrorMessage] ==== expectedError
        status ==== BadRequest
      }
    }

    "Remove a new Rule successfully" in Prop.forAll(genRuleId, genVersionId) {
      (rId, vId) =>

        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, Nil)

        val user = getUser(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          removeRule = (vrId) => EitherT.right[Future, Rejection, Unit](Future.successful(()))
        )

        Delete(s"/versions/${vId.value}/rule/${rId.value}/remove") ~> route ~> check {
          status ==== OK
        }
    }

    "Remove a new Rule fails with invalid versionId" in {
      val rId = RuleId(newId())
      val vId = VersionId("BadId")
      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      )
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, vId.value))
      val expectedError = ErrorMessage(
        rejection.message,
        Some(rejection.cause))
      Delete(s"/versions/${vId.value}/rule/remove/${rId.value}") ~> route ~> check {
        responseAs[ErrorMessage] ==== expectedError
        status ==== BadRequest
      }
    }

    "Remove a Rule fails with invalid ruleId" in {
      val rId = RuleId("BadId")
      val vId = VersionId(newId())
      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user))
      )
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, rId.value))
      val expectedError = ErrorMessage(
        rejection.message,
        Some(rejection.cause))
      Delete(s"/versions/${vId.value}/rule/${rId.value}/remove") ~> route ~> check {
        responseAs[ErrorMessage] ==== expectedError
        status ==== BadRequest
      }
    }

    "Remove a Rule fails for wrong AuthorizedUser.demandPartner" in {
      val rId = RuleId(newId())
      val vId = VersionId(newId())

      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, Nil)

      val user = getUserBadDemandPartner(rcId)

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getVersion = (vId) => {
          Future.successful(Some(vs))
        },
        removeRule = (vrId) => EitherT.right[Future, Rejection, Unit](Future.successful(()))
      )

      Delete(s"/versions/${vId.value}/rule/${rId.value}/remove") ~> route ~> check {
        status ==== Unauthorized
      }
    }

    "Remove a Rule fails for wrong AuthorizedUser.supplyPartner" in {
      val rId = RuleId(newId())
      val vId = VersionId(newId())

      val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
      val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, DemandPartnerMaxQps(MaxQps(10001)), EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, Nil)

      val user = getUserBadSupplyPartner(rcId)

      val route = versionRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getVersion = (vId) => {
          Future.successful(Some(vs))
        },
        removeRule = (vrId) => EitherT.right[Future, Rejection, Unit](Future.successful(()))
      )

      Delete(s"/versions/${vId.value}/rule/${rId.value}/remove") ~> route ~> check {
        status ==== Unauthorized
      }
    }
  }

  "Version Copy Route" should {

    "Return new Version same as specified, but with no 'published' value." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUser(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          copyVersion =
            id =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/copy") ~> route ~> check {
          val vsr = responseAs[VersionSummaryResponse]
          status === OK
          vsr.maxQps === u.maxQps
        }
    }

    "Fail for wrong AuthorizedUser.demandPartner." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUserBadDemandPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          copyVersion =
            id =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/copy") ~> route ~> check {
          status === Unauthorized
        }
    }

    "Fail for wrong AuthorizedUser.supplyPartner." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUserBadSupplyPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          copyVersion =
            id =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/copy") ~> route ~> check {
          status === Unauthorized
        }
    }
  }
  "Version Publish Route" should {

    "Return Version same as specified." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))


        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUser(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          publishVersion =
            id =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/publish") ~> route ~> check {
          val vsr = responseAs[VersionSummaryResponse]
          status === OK
          vsr.maxQps === u.maxQps
        }
    }

    "Fail for wrong AuthorizedUser.demandPartner." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUserBadDemandPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          publishVersion =
            id =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/publish") ~> route ~> check {
          status === Unauthorized
        }
    }

    "Fail for wrong AuthorizedUser.supplyPartner." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = getUserBadSupplyPartner(rcId)

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          getVersion = (vId) => {
            Future.successful(Some(vs))
          },
          publishVersion =
            id =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        Post("/versions/" + URLEncoder.encode(vId.value, "UTF-8") + "/publish") ~> route ~> check {
          status === Unauthorized
        }
    }
  }
  "Version Create Route" should {

    "Return new Version." in Prop.forAll(genQpsUpdateRequest) {
      u =>
        val updatedRules = for {
          rqu <- u.rules
        } yield RuleSummary(rqu.id, Mobile, rqu.desiredQps, MetricCount.Empty, RuleAvailableForecast(None), ruleCreatedAt, RuleName(""))

        val rId = RuleId(newId())
        val vId = VersionId(newId())
        val rcId = RouterConfigurationId(DemandPartnerId(newId()), SupplyPartnerId(newId()))
        val vs = VersionSummaryResponse(vId, rcId.dpId, rcId.spId, createdAt, modifiedAt, None, u.maxQps, EndpointAvailableForecast(None), MetricCount.Empty, MetricCount.Empty, updatedRules)

        val user = AuthenticatedUser(
          Username("bob"),
          EmailAddress("12345@test.com"),
          Fullname("bob here"),
          UserPreferences.Default,
          Organization(DemandPartnerOrganizationId(DemandPartnerId("")),""),
          DemandConfiguration(List(SupplyPartner(rcId.spId,SupplyPartnerName(""))))
        )

        val route = versionRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(user)),
          createVersion =
            rcId =>
              EitherT.right[Future, Rejection, VersionSummaryResponse](
                Future.successful(vs)
              )
        )

        val postBody = HttpEntity(`application/json`,
          Json.toJson(VersionCreateRequest(rcId.dpId,rcId.spId,u.maxQps)).toString())

        Post("/versions/create",postBody) ~> route ~> check {
          val vsr = responseAs[VersionSummaryResponse]
          status === OK
          vsr.maxQps === u.maxQps
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


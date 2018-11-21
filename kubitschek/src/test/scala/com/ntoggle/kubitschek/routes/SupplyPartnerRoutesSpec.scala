package com.ntoggle.kubitschek.routes

import java.net.URLEncoder
import java.util.UUID

import akka.http.scaladsl.model.{HttpHeader, HttpEntity}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Rejection}
import akka.http.scaladsl.testkit.RouteTest
import akka.http.specs2.Specs2Interface
import com.ntoggle.albi._
import com.ntoggle.albi.TrafficType._
import com.ntoggle.goldengate.NGen
import com.ntoggle.goldengate.elasticsearch.Size
import com.ntoggle.humber.catalog.CatalogApi.{SuggestOutputText, SuggestRequestString}
import com.ntoggle.kubitschek.api.ApiParamExtractors.ExpectedTypeNames
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.application.FeatureIndexConfig
import com.ntoggle.kubitschek.catalog.SuggestId
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.domainpersistence.mem.MemPersistence
import com.ntoggle.kubitschek.infra.ParamExtractor.ExpectedBasicTypes
import com.ntoggle.kubitschek.infra._
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.json.JsValueGenerators._
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json.{JsString, Json}

import scala.concurrent.Future
import scalaz.{-\/, \/-, \/, EitherT}
import scalaz.std.scalaFuture._

class SupplyPartnerRoutesSpec
  extends Specification
  with RouteTest
  with Specs2Interface
  with ScalaCheck {

  import PlayJsonSupportExt._
  import com.ntoggle.albi.AlbiGenerators._
  import com.ntoggle.kubitschek.domain.DomainGenerators._
  sequential

  val newId = () => UUID.randomUUID().toString
  val spId = SupplyPartnerId("6289c608-5ed9-42fa-ba8b-4d4499b9e033")
  val unAuthSpId = SupplyPartnerId("9a726b7f-e36a-441f-b83a-2581c0edfcd3")
  val badSpId = SupplyPartnerId("aabb")

  val user = AuthenticatedUser(
    Username("bob"),
    EmailAddress("12345@test.com"),
    Fullname("bob here"),
    UserPreferences.Default,
    Organization(DemandPartnerOrganizationId(DemandPartnerId("demand")), ""),
    DemandConfiguration(
      List(
        SupplyPartner(
          spId,
          SupplyPartnerName("SupplyPartner12345")))))
  def supplyPartnerRoutes(
    createSupplyPartner: (SupplyPartnerName) => ApiResponseFuture[SupplyPartner] =
    (_) => EitherT.right(Future.failed[SupplyPartner](new Exception("create supply partner should not have been called"))),
    getSupplyPartner: (SupplyPartnerId) => Future[Option[SupplyPartner]] =
    _ => Future.failed(new Exception("get supply partner should not have been called")),
    listSupplyPartner: (Offset, Limit) => Future[List[SupplyPartner]] =
    (o, l) => Future.failed(new Exception("list supply partners should not have been called")),
    getFeatures: (GetFeatureParamRequest) => ApiResponseFuture[GetFeatureResponse] =
    (_) => EitherT.right(Future.failed(new Exception("getFeatures should not have been called"))),
    getForecast: (SupplyPartnerId, DemandPartnerId, TrafficType, RuleConditions) => ApiResponseFuture[GetForecastResponse] =
    (_, _, _, _) => EitherT.right(Future.failed(new Exception("getForecast should not have been called"))),
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    _ => Future.failed(new Exception("check auth should not have been called"))
  ): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          SupplyPartnerRoutes.route(
            createSupplyPartner,
            getSupplyPartner,
            listSupplyPartner,
            getFeatures,
            getForecast,
            checkAuthentication)
        }
      }
    }

  "Supply Partner Routes" should {

    "Create supply partner and return it by ID" in Prop.forAll(genSupplyPartnerName) { name =>

      val route = supplyPartnerRoutes(
        createSupplyPartner = (_name) =>
          EitherT.right[Future, Rejection, SupplyPartner] {
            Future.successful(
              SupplyPartner(SupplyPartnerId(newId()), _name))
          })

      val req = CreateSupplyPartnerRequest(name)

      val reqBody = HttpEntity(`application/json`, Json.toJson(req).toString())

      Post("/supply-partners", reqBody) ~> route ~> check {
        responseAs[SupplyPartner].name ==== name
      }

    }

    "Return supply partner by ID" in Prop.forAll(genSupplyPartner.suchThat(_.id.id.nonEmpty)) { partner =>
      val route = supplyPartnerRoutes(
        getSupplyPartner = id =>
          Future.successful(if (id == partner.id) Some(partner) else None)
      )

      Get("/supply-partners/" + URLEncoder.encode(partner.id.id, "UTF-8")) ~> route ~> check {
        status ==== OK
        responseAs[SupplyPartner].name ==== partner.name
      }
    }

    """Reject get request to "supply-partners/{SupplyPartnerId} with invalid SupplyPartnerId""" in {
      val badId = "12345"
      val route = supplyPartnerRoutes()
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, badId))
      Get(s"/supply-partners/$badId") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))

        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    "list supply partners as array" in Prop.forAll(
      NGen.containerOfSizeRanged[List, SupplyPartner](0, 15, genSupplyPartner)) { supplyPartners =>
      val route = supplyPartnerRoutes(
        listSupplyPartner = (_, _) => Future(supplyPartners)
      )
      Get("/supply-partners?limit=10&offset=0") ~> route ~> check {
        status ==== OK
        responseAs[List[DemandPartner]] must not be null
      }
    }

    "get /supply-partners?limit=abc&offset=0 results in 400" in {
      val route = supplyPartnerRoutes(
        listSupplyPartner = (_, _) => Future(List.empty[SupplyPartner])
      )
      Get("/supply-partners?limit=abc&offset=0") ~> route ~> check {
        responseAs[ErrorMessage].cause ==== Option(Json.parse("""{"expectedType":"Int","actual":"abc"}"""))
        status ==== BadRequest
      }
    }

    "get /supply-partners?limit=10&offset=def results in 400" in {
      val route = supplyPartnerRoutes(
        listSupplyPartner = (_, _) => Future(List.empty[SupplyPartner])
      )
      Get("/supply-partners?limit=10&offset=def") ~> route ~> check {
        responseAs[ErrorMessage].cause ==== Option(Json.parse("""{"expectedType":"Int","actual":"def"}"""))
        status ==== BadRequest
      }
    }

    "Reject request on malformed JSON" in Prop.forAll(NGen.utf8String) { str =>
      val reqBody = HttpEntity(`application/json`, str)
      Post("/supply-partners", reqBody) ~> supplyPartnerRoutes() ~> check {
        status ==== BadRequest
        responseAs[ErrorMessage].message ==== "The request content was malformed."
      }

    }

    "Reject request with invalid JSON" in Prop.forAll(genObject(3)) { json =>
      val reqBody = HttpEntity(`application/json`, json.toString())
      Post("/supply-partners", reqBody) ~> supplyPartnerRoutes() ~> check {
        status ==== BadRequest
        responseAs[ErrorMessage].message ==== "JSON payload was not as expected."
      }

    }

    "Reject listing without query parameters" in {
      Get("/supply-partners") ~> supplyPartnerRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        status ==== NotFound
      }
    }

    "Reject listing without limit" in {
      Get("/supply-partners?offset=0") ~> supplyPartnerRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        error.cause ==== Some(JsString("limit"))
        status ==== NotFound
      }
    }

    "Reject listing without offset" in {
      Get("/supply-partners?limit=10") ~> supplyPartnerRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        error.cause ==== Some(JsString("offset"))
        status ==== NotFound
      }
    }

    "Return NotFound when ID provided is unknown" in Prop.forAll(
      genSupplyPartnerId.suchThat(_.id.nonEmpty)) { id =>

      val route = supplyPartnerRoutes(
        getSupplyPartner = _ =>
          Future.successful(None)
      )
      Get("/supply-partners/" + URLEncoder.encode(id.id, "UTF-8")) ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "The requested resource could not be found."
        status ==== NotFound
      }
    }

    "Get features" in Prop.forAll(
      genSupplyPartnerId,
      genDemandPartnerId,
      genTrafficType,
      Gen.size,
      genAttributeType,
      Gen.alphaStr) {
      (spid, dpid, trafficType, size, attr, q) => {
        val request = GetFeatureParamRequest(
          dpid, spid, trafficType, Size(size), attr, SuggestRequestString(q))
        val expected = GetFeatureResponse(List(FeaturePair(SuggestId("payload"), SuggestOutputText("output"))))
        val route = supplyPartnerRoutes(
          checkAuthentication = (_request) =>
            Future.successful(
              \/-(getUser(spid, dpid))),
          getFeatures = _ => EitherT.right[Future, Rejection, GetFeatureResponse](
            Future.successful(expected)))
        Get(s"/supply-partners/${spid.id}/features?trafficType=${
          TrafficType.toStringKey(trafficType)
        }&size=$size&attr=${
          AttributeType.toStringKey(attr)
        }&q=$q") ~> route ~> check {
          responseAs[GetFeatureResponse] ==== expected
        }
      }
    }

    "get features fail on invalid SupplyPartnerId" in {
      val trafficType = Mobile
      val size = 10
      val attr = AppAttr
      val q = "test"
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)))
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, badSpId.id))
      Get(s"/supply-partners/${badSpId.id}/features?trafficType=${
        TrafficType.toStringKey(trafficType)
      }&size=$size&attr=${
        AttributeType.toStringKey(attr)
      }&q=$q") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    "get features fail on invalid TrafficType" in {
      val trafficType = "web2"
      val size = 10
      val attr = "App"
      val q = "test"
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)))
      val rejection = QueryExtractorRejection(ParamExtractorError(ExpectedTypeNames.TrafficTypes, trafficType))
      Get(s"/supply-partners/${spId.id}/features?trafficType=$trafficType&size=$size&attr=$attr&q=$q") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    "get features fail on invalid AttributeType" in {
      val trafficType = "mobile"
      val size = 10
      val attr = "Country1"
      val q = "test"
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)))
      val rejection = QueryExtractorRejection(ParamExtractorError(ExpectedTypeNames.AttributeTypes, attr))
      Get(s"/supply-partners/${spId.id}/features?trafficType=$trafficType&size=$size&attr=$attr&q=$q") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    "get features fail on invalid Size" in {
      val trafficType = "mobile"
      val size = "ten"
      val attr = "Country"
      val q = "test"
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)))
      val rejection = QueryExtractorRejection(ParamExtractorError(ExpectedBasicTypes.INT, size))
      Get(s"/supply-partners/${spId.id}/features?trafficType=$trafficType&size=$size&attr=$attr&q=$q") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    "get features reject unauthorized" in {
      val trafficType = "mobile"
      val size = 10
      val attr = "App"
      val q = "test"
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(-\/(UnauthorizedAccess(AccessToken("invalid")))))
      Get(s"/supply-partners/${spId.id}/features?trafficType=$trafficType&size=$size&attr=$attr&q=$q") ~> route ~> check {
        status ==== Unauthorized
      }
    }

    "Get Forecast" in Prop.forAll(genRuleConditions, TrafficType.genTrafficType) {
      (rc, dt) => {
        val expectedResponse = GetForecastResponse(EstimatedAvailableQps(100))
        val route = supplyPartnerRoutes(
          checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
          getForecast = (_, _, _, _) =>
            EitherT.right[Future, Rejection, GetForecastResponse](Future.successful(expectedResponse)))
        val postBody = HttpEntity(`application/json`, Json.stringify(Json.toJson(rc)))
        Post(s"/supply-partners/${spId.id}/traffic-type/${TrafficType.toStringKey(dt)}/forecast", postBody) ~> route ~> check {
          responseAs[GetForecastResponse] ==== GetForecastResponse(EstimatedAvailableQps(100))
        }
      }
    }

    "post forecast reject bad request" in {
      val expectedResponse = GetForecastResponse(EstimatedAvailableQps(100))
      val dt = Mobile
      val req = """
      {
        "carrier":{
        "default":"allow",
        "undefine":"allow",
        "exceptions":["349-29"]
        }
      }
                """
      val cause = """{"obj.carrier.undefined":["error.path.missing"]}"""
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
        Future.successful(
          \/-(user)),
        getForecast = (_, _, _, rc: RuleConditions) =>
          EitherT.right[Future, Rejection, GetForecastResponse](Future.successful(expectedResponse)))
      val postBody = HttpEntity(`application/json`, Json.stringify(Json.parse(req)))
      Post(s"/supply-partners/${spId.id}/traffic-type/${TrafficType.toStringKey(dt)}/forecast", postBody) ~> route ~> check {
        status ==== BadRequest
        val error = responseAs[ErrorMessage]
        error.message ==== "JSON payload was not as expected."
        val expected = Json.parse(cause)
        error.cause ==== Some(expected)
      }
    }

    "post forecast reject unauthorized" in Prop.forAll(
      genRuleConditions, TrafficType.genTrafficType) {
      (rc, dt) => {
        val expectedResponse = GetForecastResponse(EstimatedAvailableQps(100))
        val route = supplyPartnerRoutes(
          checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
          getForecast = (_, _, _, _) =>
            EitherT.right[Future, Rejection, GetForecastResponse](Future.successful(expectedResponse)))
        val postBody = HttpEntity(`application/json`, Json.stringify(Json.toJson(rc)))
        Post(s"/supply-partners/${unAuthSpId.id}/traffic-type/${TrafficType.toStringKey(dt)}/forecast", postBody) ~> route ~> check {
          status ==== Unauthorized
        }
      }
    }

    "post forecast reject unauthenticated" in {
      val req = """{
      "test":"test"
      }"""
      val dt = Mobile
      val route = supplyPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(-\/(UnauthorizedAccess(AccessToken("invalid")))))
      val postBody = HttpEntity(`application/json`, Json.stringify(Json.parse(req)))
      Post(s"/supply-partners/${spId.id}/traffic-type/${TrafficType.toStringKey(dt)}/forecast", postBody) ~> route ~> check {
        status ==== Unauthorized
      }
    }

    "post supply-partner/{sp_id}/foo results in 404 and does not create new sp" in Prop.forAll(genSupplyPartnerId, genSupplyPartnerName) {
      (id, name) =>
        val route = supplyPartnerRoutes(
          createSupplyPartner = (_name) =>
            EitherT.right[Future, Rejection, SupplyPartner] {
            Future.successful(
              SupplyPartner(SupplyPartnerId(newId()), _name))
          })

        val req = CreateSupplyPartnerRequest(name)

        val reqBody = HttpEntity(`application/json`, Json.toJson(req).toString())

        Post(s"/supply-partners/${spId.id}/foo", reqBody) ~> route ~> check {
          val error = responseAs[ErrorMessage]
          error.message ==== "The requested resource could not be found."
          status ==== NotFound
        }
    }
  }

  private def getUser(spId:SupplyPartnerId, dpId:DemandPartnerId) = {
    AuthenticatedUser(
      Username("bob"),
      EmailAddress("12345@test.com"),
      Fullname("bob here"),
      UserPreferences.Default,
      Organization(id=DemandPartnerOrganizationId(dpId),name="demand"),
      DemandConfiguration(List(SupplyPartner(spId,SupplyPartnerName(""))))
    )
  }

}


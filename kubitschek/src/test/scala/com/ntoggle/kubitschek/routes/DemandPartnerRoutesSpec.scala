package com.ntoggle.kubitschek
package routes

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
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.NGen
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.infra.ParamExtractor.ExpectedBasicTypes
import com.ntoggle.kubitschek.infra._
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.integration.{AuthenticatedUser, UnauthorizedAccess}
import com.ntoggle.kubitschek.json.JsValueGenerators._

import com.ntoggle.kubitschek.utils._

import org.joda.time.Instant
import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import play.api.libs.json._

import scala.concurrent.Future
import scalaz.{\/-, \/, Equal, EitherT}
import scalaz.std.scalaFuture._

class DemandPartnerRoutesSpec
  extends Specification
  with RouteTest
  with Specs2Interface
  with ScalaCheck {

  import com.ntoggle.albi.AlbiGenerators._
  import PlayJsonSupportExt._

  sequential

  val newId = () => UUID.randomUUID().toString

  def demandPartnerRoutes(
    createDemandPartner: (DemandPartnerName) => ApiResponseFuture[DemandPartner] =
    _ => EitherT.right(Future.failed[DemandPartner](new Exception("create demand partner should not have been called"))),
    getDemandPartner: (DemandPartnerId) => Future[Option[DemandPartner]] =
    _ => Future.failed(new Exception("get demand partner should not have been called")),
    listDemandPartner: (Offset, Limit) => Future[List[DemandPartner]] =
    (o, l) => Future.failed(new Exception("list demand partners should not have sbeen called")),
    createRouterConfiguration: (RouterConfigurationId, RouterConfigurationRequest) =>
      ApiResponseFuture[CreateRouterConfigurationResponse] =
    (_, _) => EitherT.right(Future.failed(new Exception("Create router configuration should not have been called"))),
    getRouterConfiguration: (RouterConfigurationId) => Future[Option[RouterConfigurationResponse]] =
    _ => Future.failed(new Exception("Create router configuration should not have been called")),
    getRouterConfigurations: (DemandPartnerId) => Future[Option[List[RouterConfigurationResponse]]] =
    _ => Future.failed(new Exception("Get router configurations should not have been called")),
    getUserList: (UserListId) => Future[Option[UserList]] =
    _ => Future.failed(new Exception("getUserList should not have been called")),
    getUserListByName: (UserListName, DemandPartnerId) => Future[Option[UserList]] =
    (_,_) => Future.failed(new Exception("getUserListByName should not have been called")),
    getUserLists: (Option[DemandPartnerId], Offset, Limit) => Future[List[UserList]] =
    (_,_,_) => Future.failed(new Exception("getUserLists should not have been called")),
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser] =
    _ => Future.failed(new Exception("check auth should not have been called"))
  ): server.Route =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        rejectEmptyResponse {
          DemandPartnerRoutes.route(
            createDemandPartner,
            getDemandPartner,
            listDemandPartner,
            createRouterConfiguration,
            getRouterConfiguration,
            getRouterConfigurations,
            getUserList,
            getUserListByName,
            getUserLists,
            checkAuthentication)
        }
      }
    }

  "Demand Partner Routes" should {

    "Create demand partner should return created instance" in Prop.forAll(genDemandPartnerName) { name =>
      val route = demandPartnerRoutes(
        createDemandPartner =
          (_name) => EitherT.right[Future, Rejection, DemandPartner](
            Future.successful(DemandPartner(DemandPartnerId(newId()), _name))
          )
      )

      val postBody = HttpEntity(`application/json`,
        Json.toJson(CreateDemandPartnerRequest(name)).toString())

      Post("/demand-partners", postBody) ~> route ~> check {
        status ==== OK
        responseAs[DemandPartner].name ==== name
      }

    }

    "Return demand partner by ID" in Prop.forAll(genDemandPartner.suchThat(_.id.id.nonEmpty)) { partner =>
      val route = demandPartnerRoutes(
        getDemandPartner = id =>
          Future.successful(if (id == partner.id) Some(partner) else None)
      )

      Get("/demand-partners/" + URLEncoder.encode(partner.id.id, "UTF-8")) ~> route ~> check {
        status ==== OK
        responseAs[DemandPartner].name ==== partner.name
      }
    }

    """Reject get request to "demand-partners/{DemandPartnerId} with invalid DemandPartnerId""" in {
      val badId = "12345"
      val route = demandPartnerRoutes()
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, badId))
      Get(s"/demand-partners/$badId") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))

        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    "list demand partners as array" in Prop.forAll(
      NGen.containerOfSizeRanged[List, DemandPartner](0, 15, genDemandPartner)) { demandPartners =>
      val route = demandPartnerRoutes(
        listDemandPartner = (_, _) => Future(demandPartners)
      )
      Get("/demand-partners?limit=10&offset=0") ~> route ~> check {
        status ==== OK
        responseAs[List[DemandPartner]] must not be null
      }
    }

    "Reject request on malformed JSON" in Prop.forAll(NGen.utf8String) { str =>

      Post("/demand-partners", HttpEntity(`application/json`, str)) ~> demandPartnerRoutes() ~> check {
        status ==== BadRequest
        responseAs[ErrorMessage].message ==== "The request content was malformed."
      }
    }

    "Reject request with invalid JSON" in Prop.forAll(genObject(3)) { json =>
      val reqBody = HttpEntity(`application/json`, json.toString())
      Post("/demand-partners", reqBody) ~> demandPartnerRoutes() ~> check {
        status ==== BadRequest
        responseAs[ErrorMessage].message ==== "JSON payload was not as expected."
      }
    }

    "Reject listing without query parameters" in {
      Get("/demand-partners") ~> demandPartnerRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        status ==== NotFound
      }
    }

    "Reject listing without limit" in {
      Get("/demand-partners?offset=0") ~> demandPartnerRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        error.cause ==== Some(JsString("limit"))
        status ==== NotFound
      }
    }

    "Reject listing without offset" in {
      Get("/demand-partners?limit=10") ~> demandPartnerRoutes() ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "missing query parameter."
        error.cause ==== Some(JsString("offset"))
        status ==== NotFound
      }
    }

    "Return NotFound when ID provided is unknown" in Prop.forAll(genDemandPartnerId.suchThat(_.id.nonEmpty)) { id =>
      val route = demandPartnerRoutes(
        getDemandPartner = _ =>
          Future.successful(None)
      )
      Get("/demand-partners/" + URLEncoder.encode(id.id, "UTF-8")) ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "The requested resource could not be found."
        status ==== NotFound
      }
    }

    """
Get request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"""" in {
      val expectedId = RouterConfigurationId(
        DemandPartnerId(newId()),
        SupplyPartnerId(newId()))

      val expectedRouterCfg = RouterConfigurationResponse(
        expectedId.dpId,
        expectedId.spId,
        ConfigurationEndpoint("127.0.0.1", Port(8281)))

      val dpId = expectedId.dpId.id
      val spId = expectedId.spId.id

      val route = demandPartnerRoutes(getRouterConfiguration = id =>
        Future.successful {
          if (Equal[RouterConfigurationId].equal(expectedId, id))
            Option(expectedRouterCfg)
          else None
        })
      Get(s"/demand-partners/$dpId/supply-partners/$spId") ~> route ~> check {
        (status ==== OK) and
          (responseAs[RouterConfigurationResponse] ==== expectedRouterCfg)
      }
    }

    """
Reject get request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid DemandPartnerId""" in {
      val dpId = "FOO"
      val spId = SupplyPartnerId(newId()).id
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, dpId))

      val route = demandPartnerRoutes()
      Get(s"/demand-partners/$dpId/supply-partners/$spId") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }


    """
Reject get request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid SupplyPartnerId""" in {
      val dpId = DemandPartnerId(newId()).id
      val spId = "FOO"
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, spId))

      val route = demandPartnerRoutes()
      Get(s"/demand-partners/$dpId/supply-partners/$spId") ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }


    """
Post request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"""" in {

      val dpId = DemandPartnerId(newId())
      val spId = SupplyPartnerId(newId())
      val target = ConfigurationEndpoint("127.0.0.1", Port(8281))
      val mqps = DemandPartnerMaxQps(MaxQps(10000))
      val vId = VersionId(newId())

      val expectedResponse = CreateRouterConfigurationResponse(vId, dpId, spId, mqps, target)

      val route = demandPartnerRoutes(
        createRouterConfiguration =
          (_id, _body) => EitherT.right[Future, Rejection, CreateRouterConfigurationResponse](
            Future.successful(CreateRouterConfigurationResponse(vId, _id.dpId, _id.spId, mqps, _body.configEndpoint))
          )
      )

      val postBody = HttpEntity(`application/json`,
        Json.toJson(RouterConfigurationRequest(target, mqps)).toString())

      Post(s"/demand-partners/${dpId.id}/supply-partners/${spId.id}", postBody) ~> route ~> check {
        (status ==== OK) and
          (responseAs[CreateRouterConfigurationResponse] ==== expectedResponse)
      }
    }
    """
Reject post request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid DemandPartnerId""" in {
      val dpId = "FOO"
      val spId = SupplyPartnerId(newId()).id
      val target = ConfigurationEndpoint("127.0.0.1", Port(8281))
      val mqps = DemandPartnerMaxQps(MaxQps(10000))
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, dpId))

      val postBody = HttpEntity(`application/json`,
        Json.toJson(RouterConfigurationRequest(target, mqps)).toString())

      val route = demandPartnerRoutes()
      Post(s"/demand-partners/$dpId/supply-partners/$spId", postBody) ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }
    """
post "demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}/foo"
with non-RouterConfigurationRequest body should result in 404"
""" in {

      val dpId = DemandPartnerId(newId())
      val spId = SupplyPartnerId(newId())
      val target = ConfigurationEndpoint("127.0.0.1", Port(8281))
      val mqps = DemandPartnerMaxQps(MaxQps(10000))
      val vId = VersionId(newId())

      val expectedResponse = CreateRouterConfigurationResponse(vId, dpId, spId, mqps, target)

      val route = demandPartnerRoutes(
        createRouterConfiguration =
          (_id, _body) => EitherT.right[Future, Rejection, CreateRouterConfigurationResponse](
            Future.successful(CreateRouterConfigurationResponse(vId, _id.dpId, _id.spId, mqps, _body.configEndpoint))
          )
      )

      val postBody = HttpEntity(`application/json`, """{ "abcd": 1 }""")

      Post(s"/demand-partners/${dpId.id}/supply-partners/${spId.id}/foo", postBody) ~> route ~> check {
        status ==== NotFound
      }
    }


    """
Reject post request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid SupplyPartnerId""" in {
      val dpId = DemandPartnerId(newId()).id
      val spId = "FOO"
      val target = ConfigurationEndpoint("127.0.0.1", Port(8281))
      val mqps = DemandPartnerMaxQps(MaxQps(10000))
      val rejection = PathExtractorRejection(ParamExtractorError(ExpectedBasicTypes.UUID, spId))

      val postBody = HttpEntity(`application/json`,
        Json.toJson(RouterConfigurationRequest(target, mqps)).toString())

      val route = demandPartnerRoutes()
      Post(s"/demand-partners/$dpId/supply-partners/$spId", postBody) ~> route ~> check {
        val expectedError = ErrorMessage(
          rejection.message,
          Some(rejection.cause))
        (status ==== BadRequest) and
          (responseAs[ErrorMessage] ==== expectedError)
      }
    }

    """
Reject post request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid host""" in {
      val dpId = DemandPartnerId(newId()).id
      val spId = SupplyPartnerId(newId()).id
      val target = ConfigurationEndpoint("127.0.0.1safasdfa", Port(8281))
      val mqps = DemandPartnerMaxQps(MaxQps(10000))

      val jserror = JsError(
        (__ \ "configEndpoint") \ "host",
        ConfigurationEndpoint.InvalidHostError)
      val postBody = HttpEntity(`application/json`,
        Json.toJson(RouterConfigurationRequest(target, mqps)).toString())

      val route = demandPartnerRoutes()
      Post(s"/demand-partners/$dpId/supply-partners/$spId", postBody) ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "JSON payload was not as expected."
        error.cause ==== Some(Json.toJson(flattenJsError(jserror)))
        status ==== BadRequest
      }
    }
    """
Reject post request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid port""" in {
      val dpId = DemandPartnerId(newId()).id
      val spId = SupplyPartnerId(newId()).id
      val target = ConfigurationEndpoint("127.0.0.1", Port(99999))
      val mqps = DemandPartnerMaxQps(MaxQps(10000))

      val jserror = JsError(
        (__ \ "configEndpoint") \ "port",
        Port.InvalidPortError)
      val postBody = HttpEntity(`application/json`,
        Json.toJson(RouterConfigurationRequest(target, mqps)).toString())

      val route = demandPartnerRoutes()
      Post(s"/demand-partners/$dpId/supply-partners/$spId", postBody) ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "JSON payload was not as expected."
        error.cause ==== Some(Json.toJson(flattenJsError(jserror)))
        status ==== BadRequest
      }
    }
    """
Reject post request to
"demand-partners/{DemandPartnerId}/supply-partners/{SupplyPartnerId}"
with invalid initial MaxQps""" in {
      val dpId = DemandPartnerId(newId()).id
      val spId = SupplyPartnerId(newId()).id
      val target = ConfigurationEndpoint("127.0.0.1", Port(8234))
      val mqps = DemandPartnerMaxQps(MaxQps(-1))

      val jserror = JsError(
        __ \ "maxQps",
        MaxQps.validationError(-1))
      val postBody = HttpEntity(`application/json`,
        Json.toJson(RouterConfigurationRequest(target, mqps)).toString())

      val route = demandPartnerRoutes()
      Post(s"/demand-partners/$dpId/supply-partners/$spId", postBody) ~> route ~> check {
        val error = responseAs[ErrorMessage]
        error.message ==== "JSON payload was not as expected."
        error.cause ==== Some(Json.toJson(flattenJsError(jserror)))
        status ==== BadRequest
      }
    }
  }

  "Demand Partner Get SupplyPartners" should {

    """
Get request to
"demand-partners/{DemandPartnerId}/supply-partners"
    """ in {
      val expectedId = RouterConfigurationId(
        DemandPartnerId(newId()),
        SupplyPartnerId(newId()))

      val expectedRouterCfgResponse = List(RouterConfigurationResponse(
        expectedId.dpId,
        expectedId.spId,
        ConfigurationEndpoint("127.0.0.1", Port(8281))))

      val dpId = expectedId.dpId.id
      val spId = expectedId.spId.id

      val route = demandPartnerRoutes(getRouterConfigurations = dpId =>
        Future.successful {
          if (Equal[DemandPartnerId].equal(expectedId.dpId, dpId))
            Option(expectedRouterCfgResponse)
          else None
        })

      Get(s"/demand-partners/$dpId/supply-partners") ~> route ~> check {

        (status ==== OK) and
          (responseAs[List[RouterConfigurationResponse]] ==== expectedRouterCfgResponse)
      }

      """
      Get request to unknown DemandPartnerId
      "demand-partners/{DemandPartnerId}/supply-partners"
      """ in {

        val newDpId = DemandPartnerId(newId()).id
        val expectedEmptyResponse = List[RouterConfigurationResponse]()

        Get(s"/demand-partners/$newDpId/supply-partners") ~> route ~> check {

          (status ==== NotFound)
        }

      }

    }
  }


  "Demand Partner Get UserList" should {

    """
Get list of userlists: demand-partners/{DemandPartnerId}/user-lists"
    """ in {
      val rcId = RouterConfigurationId(
        DemandPartnerId(newId()),
        SupplyPartnerId(newId()))

      val expectedDpId = rcId.dpId
      val spId = rcId.spId

      val listId = newId()
      val unknownListId = newId()

      val expectedUserList = UserList(UserListId(listId), UserListName("userList1"), expectedDpId, CreatedInstant(new Instant(11111)), ModifiedInstant(new Instant(11111)))
      val expectedUserLists = List(expectedUserList)

      val user = utils.UserUtils.getUser(rcId)

      val route = demandPartnerRoutes(
        checkAuthentication = (_request) =>
          Future.successful(
            \/-(user)),
        getUserList = (id)
        => Future.successful {
            if (id.value == listId)
              Some(expectedUserList)
            else None },
        getUserLists = (dpId,offset,limit) =>
          Future.successful {
            expectedUserLists
          })

      Get(s"/demand-partners/${expectedDpId.id}/user-lists?offset=0&limit=10") ~> route ~> check {

        (status ==== OK) and
          (responseAs[List[UserList]] ==== expectedUserLists)
      }

"""
Get request to DemandPartnerId for specific userList : "demand-partners/{DemandPartnerId}/user-lists/listid"
      """ in {

        val newDpId = DemandPartnerId(newId())

        Get(s"/demand-partners/${expectedDpId.id}/user-lists/${listId}") ~> route ~> check {

          (status ==== OK)
        }
      }

"""
Get request to DemandPartnerId for unknown userlist : "demand-partners/{DemandPartnerId}/user-lists"
      """ in {

        val newDpId = DemandPartnerId(newId())
        val expectedEmptyResponse = List[RouterConfigurationResponse]()

        Get(s"/demand-partners/${expectedDpId.id}/user-lists/${unknownListId}") ~> route ~> check {

          (status ==== NotFound)
        }
      }

"""
Get "demand-partners/{DemandPartnerId}/foo?limit=abcd&offset=cdef results in 404"
      """ in {

        val newDpId = DemandPartnerId(newId())
        val expectedEmptyResponse = List[RouterConfigurationResponse]()

        Get(s"/demand-partners/${expectedDpId.id}/foo?limit=abcd&offset=cdef") ~> route ~> check {
          (status ==== NotFound)
        }
      }

"""
Get request to unauthorized DemandPartnerId
      "demand-partners/{DemandPartnerId}/user-lists"
      """ in {

        val newDpId = DemandPartnerId(newId())
        val expectedEmptyResponse = List[RouterConfigurationResponse]()

        Get(s"/demand-partners/${newDpId.id}/user-lists?offset=0&limit=10") ~> route ~> check {

          (status ==== Unauthorized)
        }
      }
    }
  }
}


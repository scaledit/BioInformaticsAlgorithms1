package com.ntoggle.kubitschek
package routes

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ParameterDirectives
import akka.http.scaladsl.server.{Rejection, Route}
import akka.stream.Materializer
import com.ntoggle.albi.users.UserListId
import com.ntoggle.albi.{DemandPartner, DemandPartnerId, DemandPartnerName, SupplyPartnerId}
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.infra.{ParamExtractorDirectives, SecurityDirectives, PlayJsonSupportExt, ValidatedDirectives}
import com.ntoggle.kubitschek.integration.{AuthenticatedUser, UnauthorizedAccess}
import com.ntoggle.kubitschek.services.ConfigurationErrorRejections

import scala.concurrent.Future
import scalaz.{EitherT, \/}
import scalaz.syntax.std.option._
import scalaz.syntax.equal._
import scalaz.std.scalaFuture._

import scala.concurrent.ExecutionContext.Implicits.global

object DemandPartnerRoutes
  extends ValidatedDirectives
  with ParamExtractorDirectives
  with SecurityDirectives {

  import ApiParamExtractors._
  import PlayJsonSupportExt._

  def route(
    createDemandPartner: (DemandPartnerName) => ApiResponseFuture[DemandPartner],
    getDemandPartner: (DemandPartnerId) => Future[Option[DemandPartner]],
    listDemandPartner: (Offset, Limit) => Future[List[DemandPartner]],
    createRouterConfiguration: (RouterConfigurationId, RouterConfigurationRequest) =>
      ApiResponseFuture[CreateRouterConfigurationResponse],
    getRouterConfiguration: (RouterConfigurationId) => Future[Option[RouterConfigurationResponse]],
    getRouterConfigurations: (DemandPartnerId) => Future[Option[List[RouterConfigurationResponse]]],
    getUserList: (UserListId) => Future[Option[UserList]],
    getUserListByName: (UserListName, DemandPartnerId) => Future[Option[UserList]],
    getUserLists: (Option[DemandPartnerId], Offset, Limit) => Future[List[UserList]],
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser]
  )(
    implicit materializer: Materializer): Route =
    pathPrefix("demand-partners") {
      (post & pathEnd & validateJson[CreateDemandPartnerRequest]) { dpReq =>
        onApiResponseFutureComplete(createDemandPartner(dpReq.name))(complete(_))
      } ~
        (get & path(pathValue[DemandPartnerId])) { dpId =>
          onComplete(getDemandPartner(dpId)) { dp =>
            complete(dp)
          }
        } ~
        path(pathValue[DemandPartnerId] /
          "supply-partners") {
          (dpId) =>
            (get & pathEnd) {
              onComplete(getRouterConfigurations(dpId)) {
                complete(_)
              }
            }
        } ~
        pathPrefix(pathValue[DemandPartnerId] /
          "supply-partners" /
          pathValue[SupplyPartnerId]) { (dpId, spId) =>
            val id = RouterConfigurationId(dpId, spId)
              (post & pathEnd & validateJson[RouterConfigurationRequest] & handleRejections(rejectionHandler)) { pubReq =>
                onApiResponseFutureComplete(createRouterConfiguration(id, pubReq)) {
                  complete(_)
                }
              } ~
                (get & pathEnd) {
                  onComplete(getRouterConfiguration(id)) { m =>
                    complete(m)
                  }
                }
        } ~
        pathPrefix(pathValue[DemandPartnerId] / "user-lists") { dpId =>
          authenticate(checkAuthentication) { user => {
            authorizeDp(dpId, user) { userDpId =>
              (get & path(pathValue[UserListId]) & pathEnd) { ulId =>
                onApiResponseFutureComplete(getUserListForAuthorizedUser(ulId, user, getUserList)) { ul =>
                  complete(ul)
                }
              } ~
              (get & pathEnd & queryparam(Tuple2(
                "limit".as[Int],
                "offset".as[Int]))) { (limit, offset) =>
                onComplete(getUserLists(Some(user.demandPartner), Offset(offset), Limit(limit))) {
                  complete(_)
                }
              }
          }
          }
          }
        } ~
        (get & pathEnd & queryparam(Tuple2("limit".as[Int], "offset".as[Int]))) { (limit, offset) =>
          onComplete(listDemandPartner(Offset(offset), Limit(limit))) { dp =>
            complete(dp)
          }
        }
    }


  private def getUserListForAuthorizedUser(
    id: UserListId,
    user: AuthenticatedUser,
    getUserList: UserListId => Future[Option[UserList]]): ApiResponseFuture[UserList] = {

    val (userDpId, userSps) = user.credentials()

    for {
      r <- EitherT.eitherT(getUserList(id).map(_.\/>(ConfigurationError.userListNotFound(id))))
        .leftMap(ConfigurationErrorRejections.rejection)

      result <- {
        if (userDpId === r.dpId) EitherT.right[Future, Rejection, UserList](Future.successful(r))
        else {
          EitherT.left[Future, Rejection, UserList](Future.successful(ConfigurationErrorRejections.rejection(ConfigurationError.resourceNotAuthorized())))
        }
      }

    } yield result
  }

  private def getUserListByNameForAuthorizedUser(
    name: UserListName,
    dpId: DemandPartnerId,
    user: AuthenticatedUser,
    getUserListByName: (UserListName, DemandPartnerId) => Future[Option[UserList]]): ApiResponseFuture[UserList] = {

    val (userDpId, userSps) = user.credentials()

    for {
      r <- EitherT.eitherT(getUserListByName(name, dpId).map(_.\/>(ConfigurationError.userListNotFoundByName(name))))
        .leftMap(ConfigurationErrorRejections.rejection)

    } yield r
  }

}

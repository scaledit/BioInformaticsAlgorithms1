package com.ntoggle.kubitschek.routes

import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{StandardRoute, Rejection, Route}
import akka.stream.Materializer
import com.ntoggle.albi.{DemandPartnerId, SupplyPartnerId}
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.api.VersionSummaryResponse
import com.ntoggle.kubitschek.infra._
import com.ntoggle.kubitschek.integration.{AuthenticatedUser, UnauthorizedAccess}
import com.ntoggle.kubitschek.services.ConfigurationErrorRejections

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Try, Failure, Success}
import scalaz.{EitherT, \/}

import scalaz.syntax.std.option._
import scalaz.syntax.equal._
import scalaz.std.scalaFuture._


object VersionRoutes
  extends ValidatedDirectives
  with ParamExtractorDirectives
  with SecurityDirectives {
  import PlayJsonSupportExt._
  import com.ntoggle.kubitschek.api.ApiParamExtractors._
  import com.ntoggle.kubitschek.infra.CustomErrorHandlers._

  def route(

    getVersion: (VersionId) => Future[Option[VersionSummaryResponse]],
    listVersions: (Option[DemandPartnerId], Option[SupplyPartnerId], Offset, Limit) => Future[List[Version]],
    setQps: (VersionId, VersionQpsUpdateRequest) => ApiResponseFuture[VersionSummaryResponse],
    createVersion: (VersionCreateRequest) => ApiResponseFuture[VersionSummaryResponse],
    copyVersion: (VersionId) => ApiResponseFuture[VersionSummaryResponse],
    publishVersion: (VersionId) => ApiResponseFuture[VersionSummaryResponse],
    saveRule: (VersionId, CreateRuleRequest) => ApiResponseFuture[GetRuleResponse],
    replaceRule: (VersionId, RuleId, ReplaceRuleRequest) => ApiResponseFuture[GetRuleResponse],
    removeRule: (VersionRuleId) => ApiResponseFuture[Unit],
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser]
    )(implicit materializer: Materializer): Route =
    pathPrefix("versions") {

      authenticate(checkAuthentication) { user => {
        // if you don't have a 'local' rejectionHandler, the default is to try the rest of the route paths.
        (post & path("create") & pathEnd & handleRejections(rejectionHandler) & validateJson[UserVersionCreateRequest] ) { vcr =>
          authorizeSp(vcr.spId, user)  { (userDpId, userSpId) =>
              onApiResponseFutureComplete (createVersion (VersionCreateRequest(userDpId, userSpId, vcr.maxQps)) ) {
                v =>
                  complete (v)
              }
            }
        } ~
        (get & path("summary" / pathValue[VersionId]) & pathEnd) { vId =>
          onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) { v => complete(v)}
        } ~
        (get & pathEnd & queryparam(Tuple3(
          "spId".as[SupplyPartnerId].?,
          "limit".as[Int],
          "offset".as[Int]))) {
          (spId, limit, offset) =>
            authorizeOptionalSp(spId, user)  { (userDpId, userSpId) =>
              onComplete(listVersions(Option(userDpId), userSpId, Offset(offset), Limit(limit))) {
                versions => filterVersions(versions, user)
              }
            }
        } ~
        (post & path(pathValue[VersionId] / "qps") & pathEnd & validateJson[VersionQpsUpdateRequest]) {
          (vId,req) =>
          onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) {
            case Success(_) =>
              onApiResponseFutureComplete(setQps(vId, req)) { qpsSummary =>
                complete(qpsSummary)
              }
            case tvsr => complete(tvsr)
          }
        } ~
        (post & path(pathValue[VersionId] / "copy") & pathEnd) {
          (vId) => {
            onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) {
              case Success(_) =>
                onApiResponseFutureComplete(copyVersion(vId)) { vs =>
                  complete(vs)
                }
              case tvsr => complete(tvsr)
            }
          }
        } ~
        (post & path(pathValue[VersionId] / "publish")) {
          (vId) => {
            onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) {
              case Success(_) =>
                onApiResponseFutureComplete(publishVersion(vId)) { vsr =>
                  complete(vsr)
                }
              case tvsr => complete(tvsr)
            }
          }
        } ~
        (post & path(pathValue[VersionId] / "rule"/ "add" ) & pathEnd & handleRejections(rejectionHandler) & validateJson[CreateRuleRequest]) { (vId, ruleReq) =>
          onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) {
            case Success(_) =>
              onApiResponseFutureComplete(saveRule(vId, ruleReq)) { tgrr =>
                complete(tgrr)
              }
            case tvsr => complete(tvsr)
          }
        } ~
        (post & path(pathValue[VersionId] / "rule" / pathValue[RuleId] / "replace" ) & pathEnd & validateJson[ReplaceRuleRequest]) { (vId, rId, ruleReq) =>
          onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) {
            case Success(_) =>
              onApiResponseFutureComplete(replaceRule(vId, rId, ruleReq)) { tgrr =>
                complete(tgrr)
              }
            case tvsr => complete(tvsr)
          }
        } ~
        (delete & path(pathValue[VersionId] / "rule"  / pathValue[RuleId] / "remove") & pathEnd) { (vId, rId) =>
          onApiResponseFutureComplete(getVersionForAuthorizedUser(vId, user, getVersion)) {
            case Success(_) =>
              onApiResponseFutureComplete(removeRule(VersionRuleId(vId, rId))) {
                case Success(_) => complete(StatusCodes.OK)
                case Failure(t) => complete(t)
              }
            case tvsr => complete(tvsr)
          }
        }
      }
      }
    }

  // Ensure that AuthorizedUser has access to the Version specified by VersionId.
  private def getVersionForAuthorizedUser(vId: VersionId,
    user: AuthenticatedUser,
    getVersion: (VersionId) => Future[Option[VersionSummaryResponse]]): ApiResponseFuture[VersionSummaryResponse] = {

    val (userDpId, userSps) = user.credentials()
    val userSpIds = userSps.map(_.id)

    for {

      vsr <- EitherT.eitherT(getVersion(vId).map(_.\/>(ConfigurationError.versionNotFound(vId))))
        .leftMap(ConfigurationErrorRejections.rejection)
      dpId <- EitherT.right(Future.successful(vsr.dpId))
      spId <- EitherT.right(Future.successful(vsr.spId))
      result <- {
        if (userDpId === dpId && userSpIds.contains(spId)) EitherT.right[Future, Rejection, VersionSummaryResponse](Future.successful(vsr))
        else {
          EitherT.left[Future, Rejection, VersionSummaryResponse] (Future.successful(AuthorizationRejection(s"Not authorized to access version: '${vId.value}'")))
        }
      }

    } yield result

  }

  private def filterVersions(versionSummaries: Try[List[Version]], user: AuthenticatedUser): StandardRoute = {

    versionSummaries match {
      case Success(vsr) => complete(
        vsr.filter(v =>
          user.supplyPartners().map(_.id).contains(v.routerConfigurationId.spId)))
      case _ => complete(versionSummaries)
    }
  }
}


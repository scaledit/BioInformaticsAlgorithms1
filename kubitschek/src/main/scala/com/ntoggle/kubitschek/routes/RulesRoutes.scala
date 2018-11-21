package com.ntoggle.kubitschek.routes

import akka.http.scaladsl.model.{StatusCodes, HttpHeader}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Rejection, Route}
import akka.stream.Materializer
import com.ntoggle.albi.{DemandPartnerId, SupplyPartnerId}
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.infra.{AuthorizationRejection, SecurityDirectives, PlayJsonSupportExt, ValidatedDirectives}
import com.ntoggle.kubitschek.integration.{AuthenticatedUser, UnauthorizedAccess}
import com.ntoggle.kubitschek.services.ConfigurationErrorRejections

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.{EitherT, \/}
import scalaz.syntax.std.option._
import scalaz.std.scalaFuture._


object RulesRoutes
  extends ValidatedDirectives
  with SecurityDirectives {

  import PlayJsonSupportExt._
  import com.ntoggle.kubitschek.api.ApiParamExtractors._
  import com.ntoggle.kubitschek.infra.CustomErrorHandlers._

  def route(
    getRule: (RuleId) => Future[Option[GetRuleResponse]],
    checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser],
    listVersionsForRule: (RuleId,
      DemandPartnerId,
      List[SupplyPartnerId],
      Offset,
      Limit) => Future[List[Version]]
    )(implicit materializer: Materializer): Route =
    pathPrefix("rules") {
      authenticate(checkAuthentication) { user =>
        (get & path(pathValue[RuleId])) { ruleId =>
          onApiResponseFutureComplete(getVersionForAuthorizedUser(ruleId, user, getRule, listVersionsForRule)) { tv =>

            tv match {
              case Success(_) =>
                onComplete(getRule(ruleId)) {
                case Success(r) => complete(r)
                case Failure(t) => complete(t)
                      }
              case _ => complete(tv)
              }
            }
        }
      }
    }

  // Ensure that AuthorizedUser has access at least one Version specified by RuleId.
  private def getVersionForAuthorizedUser(rId: RuleId,
    user: AuthenticatedUser,
    getRule: (RuleId) =>Future[Option[GetRuleResponse]],
    listVersionsForRule: (RuleId, DemandPartnerId, List[SupplyPartnerId], Offset, Limit) => Future[List[Version]]) : ApiResponseFuture[List[Version]] = {

    val (userDpId, userSps) = user.credentials()
    val userSpIds = userSps.map(_.id)

    for {
      r <- EitherT.eitherT(getRule(rId).map(_.\/>(ConfigurationError.ruleNotFound(rId))))
        .leftMap(ConfigurationErrorRejections.rejection)

      vl <- EitherT.right(listVersionsForRule(rId, userDpId, userSpIds, Offset(0), Limit(1)))

      result <- {
        if (vl.length == 1) EitherT.right[Future, Rejection, List[Version]](Future.successful(vl))
        else {
          EitherT.left[Future, Rejection, List[Version]] (Future.successful(ConfigurationErrorRejections.rejection(ConfigurationError.resourceNotAuthorized())))
        }
      }

    } yield  result

  }

}

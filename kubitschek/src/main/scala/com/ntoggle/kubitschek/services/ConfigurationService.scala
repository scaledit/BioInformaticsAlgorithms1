package com.ntoggle.kubitschek
package services

import com.ntoggle.albi.DemandPartnerId
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.domain._
import org.joda.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.scalaFuture._
import scalaz.{ISet, EitherT, \/}

object ConfigurationService {
  def getRouterConfiguration(
    getRouterConfig: RouterConfigurationId => Future[Option[RouterConfiguration]])
    (implicit ctx: ExecutionContext):
  (RouterConfigurationId) => Future[Option[RouterConfigurationResponse]] = {

    id =>
      getRouterConfig(id).map(_.map(
        result => RouterConfigurationResponse(
          result.id.dpId,
          result.id.spId,
          result.target)))
  }

  def getRouterConfigurations(
    getRouterConfigs: DemandPartnerId => Future[Option[List[RouterConfiguration]]])
    (implicit ctx: ExecutionContext):
  (DemandPartnerId) => Future[Option[List[RouterConfigurationResponse]]] = {

    id =>
    getRouterConfigs(id).map(_.map(_.map(
        result => RouterConfigurationResponse(
          result.id.dpId,
          result.id.spId,
          result.target))))
  }

  def createRouterConfiguration(
    newId: () => Future[String],
    now: () => Future[Instant],
    addRouterConfiguration: (RouterConfiguration, VersionId, CreatedInstant, DemandPartnerMaxQps) =>
      Future[ConfigurationError \/ RouterConfiguration]
    )(implicit ctx: ExecutionContext): (RouterConfigurationId, RouterConfigurationRequest) =>
    ApiResponseFuture[CreateRouterConfigurationResponse] =
    (id, req) =>
      for {
        initialVersion <- EitherT.right(newId()).map(VersionId.apply)
        now <- EitherT.right(now()).map(CreatedInstant.apply)
        cfg = RouterConfiguration(
          id,
          req.configEndpoint)
        result <- EitherT.eitherT(
          addRouterConfiguration(
            cfg,
            initialVersion,
            now,
            req.maxQps)).leftMap(ConfigurationErrorRejections.rejection)
      } yield CreateRouterConfigurationResponse(
        initialVersion,
        result.id.dpId,
        result.id.spId,
        req.maxQps,
        result.target)
}

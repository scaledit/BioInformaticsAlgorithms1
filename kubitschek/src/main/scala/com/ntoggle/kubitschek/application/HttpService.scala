package com.ntoggle.kubitschek
package application

import java.io.File
import java.lang.Thread.UncaughtExceptionHandler
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.albi.TrafficType
import com.ntoggle.goldengate.elasticsearch._
import com.ntoggle.humber.catalog.ESCatalog._
import com.ntoggle.humber.estimation.EstimateConfig
import com.ntoggle.kubitschek.api._
import com.ntoggle.kubitschek.catalog.FeatureIndexClient
import com.ntoggle.kubitschek.domain._
import com.ntoggle.kubitschek.estimation.Estimate
import com.ntoggle.kubitschek.infra.CustomErrorHandlers._
import com.ntoggle.kubitschek.integration._
import com.ntoggle.kubitschek.routes._
import com.ntoggle.kubitschek.services._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.LazyLogging
import monocle.Lens
import monocle.macros.GenLens
import org.joda.time.Instant

import scala.App
import scala.concurrent.Future
import scalaz.\/
import scalaz.concurrent.Task

class ApiService(
  createDemandPartner: (DemandPartnerName) => ApiResponseFuture[DemandPartner],
  getDemandPartner: (DemandPartnerId) => Future[Option[DemandPartner]],
  listDemandPartner: (Offset, Limit) => Future[List[DemandPartner]],
  createRouterConfiguration: (RouterConfigurationId, RouterConfigurationRequest) =>
    ApiResponseFuture[CreateRouterConfigurationResponse],
  getRouterConfiguration: (RouterConfigurationId) => Future[Option[RouterConfigurationResponse]],
  getRouterConfigurations: (DemandPartnerId) => Future[Option[List[RouterConfigurationResponse]]],
  createSupplyPartner: (SupplyPartnerName) =>
    ApiResponseFuture[SupplyPartner],
  getSupplyPartner: SupplyPartnerId => Future[Option[SupplyPartner]],
  listSupplyPartners: (Offset, Limit) => Future[List[SupplyPartner]],
  getFeatures: GetFeatureParamRequest => ApiResponseFuture[GetFeatureResponse],
  //featureConfig: FeatureIndexConfig,
  getForecast: (SupplyPartnerId, DemandPartnerId, TrafficType, RuleConditions) => ApiResponseFuture[GetForecastResponse],
  getVersion: (VersionId) => Future[Option[VersionSummaryResponse]],
  listVersions: (Option[DemandPartnerId], Option[SupplyPartnerId], Offset, Limit) => Future[List[Version]],
  setQps: (VersionId, VersionQpsUpdateRequest) => ApiResponseFuture[VersionSummaryResponse],
  createVersion: (VersionCreateRequest) => ApiResponseFuture[VersionSummaryResponse],
  copyVersion: (VersionId) => ApiResponseFuture[VersionSummaryResponse],
  publish: (VersionId) => ApiResponseFuture[VersionSummaryResponse],
  saveRule: (VersionId, CreateRuleRequest) => ApiResponseFuture[GetRuleResponse],
  replaceRule: (VersionId, RuleId, ReplaceRuleRequest) => ApiResponseFuture[GetRuleResponse],
  removeRule: (VersionRuleId) => ApiResponseFuture[Unit],
  getRule: (RuleId) => Future[Option[GetRuleResponse]],
  apiDocConfig: ApiDocConfig,
  getOAuthToken: (Username, Password) => ApiResponseFuture[OAuthToken],
  userDetails: AuthenticatedUser => UserDetails,
  demandConfig: AuthenticatedUser => DemandConfiguration,
  checkAuthentication: Seq[HttpHeader] => Future[UnauthorizedAccess \/ AuthenticatedUser],
  listVersionsForRule: (RuleId, DemandPartnerId, List[SupplyPartnerId], Offset, Limit) => Future[List[Version]],
  getUserList: (UserListId) => Future[Option[UserList]],
  getUserListByName: (UserListName, DemandPartnerId) => Future[Option[UserList]],
  getUserLists: (Option[DemandPartnerId], Offset, Limit) => Future[List[UserList]]
)(implicit materializer: Materializer) extends LazyLogging {

  val routes =
    handleExceptions(exceptionHandler) {
      handleRejections(rejectionHandler) {
        logRequestResult("kubitschek-http-service") {
          pathPrefix("api" / "v1") {
            ApiDocRoutes.route(apiDocConfig) ~
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
                  checkAuthentication
                ) ~
                  SupplyPartnerRoutes.route(
                    //featureConfig,
                    createSupplyPartner,
                    getSupplyPartner,
                    listSupplyPartners,
                    getFeatures,
                    getForecast,
                    checkAuthentication
                  ) ~
                  VersionRoutes.route(
                    getVersion,
                    listVersions,
                    setQps,
                    createVersion,
                    copyVersion,
                    publish,
                    saveRule,
                    replaceRule,
                    removeRule,
                    checkAuthentication
                  ) ~
                  RulesRoutes.route(
                    getRule,
                    checkAuthentication,
                    listVersionsForRule)
              } ~
              pathPrefix("oauth2") {
                OAuthRoutes.route(
                  getOAuthToken,
                  checkAuthentication
                )
              } ~
              AccountRoutes.route(
                checkAuthentication,
                userDetails,
                demandConfig)
          }
        }
      }
    }
}

object HttpService
  extends App
  with LazyLogging {

  Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler {
    def uncaughtException(t: Thread, e: Throwable): Unit = {
      logger.error(s"Shutting down due to uncaught exception in thread = $t", e)
      System.exit(1)
    }
  })

  case class Input(config: Option[File])
  object Input {
    val Default = Input(None)
    val _config: Lens[Input, Option[File]] = GenLens[Input](_.config)
  }
  val parser = new scopt.OptionParser[Input]("Kubitschek") {
    head("nToggle API")
    opt[File]('c', "config") valueName "<file>" required() action {
      (p, input) =>
        logger.info(s"Using config file '$p'")
        Input._config.set(Some(p))(input)
    } text "config file required, e.g. '/config/kubitscheck.conf'"
  }

  val rootConfig: Config = parser.parse(args, Input.Default) match {
    case Some(Input(Some(i))) =>
      val configFromFile = ConfigFactory.parseFile(i)
      val mergedConfig = ConfigFactory.load(configFromFile)
      mergedConfig.checkValid(ConfigFactory.empty())
      mergedConfig
    case other =>
      parser.showUsageAsError
      System.exit(1)
      throw new RuntimeException("Error") // necessary for type check
  }

  implicit val system = ActorSystem("kubitschek", rootConfig)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val kubitschekConfig =
    KubitschekConfig.fromConfig(rootConfig.getConfig("kubitschek"))

  logger.info(s"Using stormpath config    '${kubitschekConfig.stormPathClientConfig}'")
  logger.info(s"Using persistence config   '${kubitschekConfig.persistenceConfig}'")
  logger.info(s"Using http service config '${kubitschekConfig.httpServiceConfig}'")
  logger.info(s"Using api doc config      '${kubitschekConfig.apiDocConfig}'")
  logger.info(s"Using feature config      '${kubitschekConfig.featureConfig}'")
  logger.info(s"Using ES Config           '${kubitschekConfig.esConfig}'")
  logger.info(s"Using estimation config   '${kubitschekConfig.estimationConfig}'")

  // may need to use different execution context here because it blocks
  val persistence = kubitschekConfig.persistenceConfig.persistence

  val esClient = ESClient(
    kubitschekConfig.esConfig.remoteClient())
  val featureIndexClient = new FeatureIndexClient(
    persistence.listUserLists,
    autoCompleteApps(esClient, kubitschekConfig.featureConfig.appIndex),
    autoCompleteHandset(esClient, kubitschekConfig.featureConfig.handsetIndex),
    autoCompleteOs(esClient, kubitschekConfig.featureConfig.osIndex),
    autoCompleteCity(esClient, kubitschekConfig.featureConfig.cityIndex),
    autoCompleteCarrier(esClient, kubitschekConfig.featureConfig.carrierIndex))
  //  val featureIndexClient = FeatureIndexStub.stubData // STUB

  val estimateConfig = EstimateConfig(
    kubitschekConfig.estimationConfig.estimationDuration,
    kubitschekConfig.estimationConfig.bidRequestIndex,
    kubitschekConfig.estimationConfig.secondsIndex,
    esClient,
    Task(new Instant()))
  val estimateRule = Estimate.estimate(estimateConfig)
  //  val estimateRule = EstimateStub.estimateRule // STUB

  val estimateFromRuleId = Estimate.estimateFromRuleId(
    estimateConfig,
    persistence.getRule(_))
  //  val estimateFromRuleId = EstimateStub.estimateFromRuleId // STUB

  val estimateFromEndpoint = Estimate.estimateEndpointAvailable(estimateConfig)
  //  val estimateFromEndpoint = EstimateStub.estimateEndpointAvailable // STUB

  //  val featureIndexClient = FeatureIndexStub.stubData

  val newId = () => Future.successful(UUID.randomUUID().toString)
  val newInstant = () => Future.successful(new Instant())

  val stormPathClient = StormPathClient.fromConfig(
    kubitschekConfig.stormPathClientConfig)

  // temporary - want to merge in conf. changes from develop before adding to proper home.
  val metrics = new MetricsService(
    kubitschekConfig.metricsConfig, executionContext)

  val httpService = new ApiService(
    DemandPartnerService.create(newId, persistence.addDemandPartner)(system.dispatcher),
    DemandPartnerService.get(persistence.getDemandPartner),
    DemandPartnerService.list(persistence.listDemandPartners),
    ConfigurationService.createRouterConfiguration(
      newId,
      newInstant,
      persistence.addRouterConfiguration)(system.dispatcher),
    ConfigurationService.getRouterConfiguration(
      persistence.getRouterConfiguration)(system.dispatcher),
    ConfigurationService.getRouterConfigurations(
      persistence.getRouterConfigurations)(system.dispatcher),
    SupplyPartnerService.create(newId, persistence.addSupplyPartner)(system.dispatcher),
    SupplyPartnerService.get(persistence.getSupplyPartner),
    SupplyPartnerService.list(persistence.listSupplyPartners)(system.dispatcher),
    FeatureService.get(featureIndexClient.autoComplete)(system.dispatcher),
    //kubitschekConfig.featureConfig,
    ForecastService.get(estimateRule),
    VersionService.get(
      persistence.getVersionSummary,
      estimateFromEndpoint,
      metrics.requests,
      metrics.bids,
      metrics.ruleActuals,
      estimateFromRuleId)(system.dispatcher),
    persistence.listVersions,
    VersionService.setQps(
      newInstant,
      persistence.setVersionQps,
      VersionService.get(
        persistence.getVersionSummary,
        estimateFromEndpoint,
        metrics.requests,
        metrics.bids,
        metrics.ruleActuals,
        estimateFromRuleId)(system.dispatcher),
      estimateFromEndpoint,
      metrics.requests,
      metrics.bids,
      metrics.ruleActuals,
      estimateFromRuleId)(system.dispatcher),
    VersionService.create(
      newId,
      newInstant,
      estimateFromEndpoint,
      metrics.requests,
      metrics.bids,
      metrics.ruleActuals,
      estimateFromRuleId,
      persistence.addVersion)(system.dispatcher),
    VersionService.copy(
      newId,
      newInstant,
      persistence.copyVersion,
      estimateFromEndpoint,
      metrics.requests,
      metrics.bids,
      metrics.ruleActuals,
      estimateFromRuleId)(system.dispatcher),
    VersionService.publish(
      newId,
      newInstant,
      PublishingService.publish(
        persistence.publishVersion,
        persistence.getRouterConfiguration,
        PublishingService.publishConfiguration(
          persistence.getRule,
          estimateFromRuleId)),
      estimateFromEndpoint,
      metrics.requests,
      metrics.bids,
      metrics.ruleActuals,
      estimateFromRuleId)(system.dispatcher),
    RuleService.save(newId, persistence.saveRuleToVersion)(system.dispatcher),
    RuleService.replace(newId, persistence.updateRuleInVersion)(system.dispatcher),
    RuleService.remove(persistence.removeRuleFromVersion),
    RuleService.get(persistence.getRule),
    kubitschekConfig.apiDocConfig,
    AuthenticationService.authenticate(stormPathClient.generateToken),
    AuthenticationService.userDetails,
    AuthenticationService.demandConfig,
    AuthenticationService.checkAuthentication(stormPathClient.authToken),
    RuleService.listVersionsForRule(persistence.listVersionsForRule),
    UserListService.get(persistence.getUserList)(system.dispatcher),
    UserListService.getByName(persistence.getUserListByName)(system.dispatcher),
    UserListService.getUserLists(persistence.listUserLists)(system.dispatcher)
  )

  Http().bindAndHandle(
    httpService.routes,
    kubitschekConfig.httpServiceConfig.interface,
    kubitschekConfig.httpServiceConfig.port)
}


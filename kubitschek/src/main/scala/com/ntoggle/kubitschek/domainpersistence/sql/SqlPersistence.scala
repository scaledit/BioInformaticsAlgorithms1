package com.ntoggle.kubitschek
package domainpersistence
package sql

import java.util.UUID

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.slick.SqlClient
import com.ntoggle.kubitschek.domain._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.slick.jdbc.JdbcBackend
import scalaz._
import scalaz.std.anyVal.intInstance
import scalaz.std.option._
import scalaz.std.list._
import scalaz.syntax.either._
import scalaz.syntax.equal._
import scalaz.syntax.std.list._
import scalaz.syntax.std.option._
import scalaz.syntax.traverse._

object SqlPersistence {
  def apply(
    client: SqlClient,
    sqlExecutionContext: ExecutionContext): Persistence =
    new SqlPersistence(client, () => Future.successful(SqlRuleConditionId(UUID.randomUUID().toString)))(sqlExecutionContext)
}

class SqlPersistence(
  client: SqlClient,
  newRuleConditionId: () => Future[SqlRuleConditionId])(implicit executionContext: ExecutionContext)
  extends Persistence
  with LazyLogging {

  val queries = SqlQueries

  def addDemandPartner(
    dp: DemandPartner): Future[ConfigurationError \/ DemandPartner] =
    queries.normalizeFutureExceptions {
      client.execTx { implicit s =>
        queries.DemandPartners.insert(dp)
        dp.right
      }
    }.recover {
      case e: UniqueConstraintViolation =>
        if (e.constraint.exists(_.nameMatches(".*_name_.*".r)))
          ConfigurationError.demandPartnerNameAlreadyExists(dp.name).left
        else
          ConfigurationError.demandPartnerAlreadyExists(dp.id).left
    }

  def getDemandPartner(id: DemandPartnerId): Future[Option[DemandPartner]] =
    client.exec(queries.DemandPartners.get(id)(_))

  def setDemandPartner(dp: DemandPartner): Future[ConfigurationError \/ DemandPartner] =
    queries.normalizeFutureExceptions {
      client.execTxEither { implicit s =>
        val rows = queries.DemandPartners.update(dp)
        if (rows === 1) dp.right
        else ConfigurationError.demandPartnerNotFound(dp.id).left
      }
    }.recover {
      case e: UniqueConstraintViolation =>
        ConfigurationError.demandPartnerNameAlreadyExists(dp.name).left
    }

  def listDemandPartners(offset: Offset, limit: Limit): Future[List[DemandPartner]] =
    client.exec(queries.DemandPartners.list(limit, offset)(_))

  def deleteDemandPartner(id: DemandPartnerId): Future[Unit] =
    queries.normalizeFutureExceptions {
      client.execTx { implicit s =>
        queries.DemandPartners.delete(id)
      }
    }

  def addSupplyPartner(sp: SupplyPartner): Future[ConfigurationError \/ SupplyPartner] =
    queries.normalizeFutureExceptions {
      client.execTx { implicit s =>
        queries.SupplyPartners.insert(sp)
        sp.right
      }
    }.recover {
      case e: UniqueConstraintViolation =>
        if (e.constraint.exists(_.nameMatches(".*_name_.*".r)))
          ConfigurationError.supplyPartnerNameAlreadyExists(sp.name).left
        else
          ConfigurationError.supplyPartnerAlreadyExists(sp.id).left
    }

  def getSupplyPartner(id: SupplyPartnerId): Future[Option[SupplyPartner]] =
    client.exec { implicit s =>
      queries.SupplyPartners.get(id)
    }

  def setSupplyPartner(sp: SupplyPartner): Future[ConfigurationError \/ SupplyPartner] =
    queries.normalizeFutureExceptions {
      client.execTxEither { implicit s =>
        for {
          nameResult <- {
            val rows = queries.SupplyPartners.update(sp)
            if (rows === 1) sp.right
            else ConfigurationError.supplyPartnerNotFound(sp.id).left
          }
        } yield sp
      }
    }.recover {
      case e: UniqueConstraintViolation =>
        ConfigurationError.supplyPartnerNameAlreadyExists(sp.name).left
    }

  def listSupplyPartners(offset: Offset, limit: Limit): Future[List[SupplyPartner]] =
    client.exec { implicit s =>
      queries.SupplyPartners.list(limit, offset)
    }

  def deleteSupplyPartner(id: SupplyPartnerId): Future[Unit] =
    queries.normalizeFutureExceptions {
      client.execTx { implicit s =>
        queries.SupplyPartners.delete(id)
      }
    }

  def addVersion(
    id: VersionId,
    cfgId: RouterConfigurationId,
    created: CreatedInstant,
    maxQps: DemandPartnerMaxQps): Future[ConfigurationError \/ Version] = {
    val newVersion = Version(id, cfgId, created, ModifiedInstant(created.value), None, maxQps)
    queries.normalizeFutureExceptions {
      client.execTx { implicit s =>
        queries.Versions.insert(newVersion)
          .right.map(_ => newVersion)
      }
    }.recover {
      case e: UniqueConstraintViolation =>
        ConfigurationError.versionAlreadyExists(newVersion.id).left
      case e: ForeignKeyViolationError =>
        ConfigurationError.routerConfigurationNotFound(
          newVersion.routerConfigurationId).left
    }
  }

  def getVersion(id: VersionId): Future[Option[Version]] =
    client.exec(queries.Versions.get(id)(_))

  def getVersionSummary(id: VersionId): Future[Option[VersionSummary]] =
    client.exec { implicit s =>
      queries.Versions.get(id).map {
        v =>
          VersionSummary.fromVersionAndSummaries(
            v,
            queries.VersionRules.listSummariesByVersion(id))
      }
    }

  def setVersionQps(
    vId: VersionId,
    maxQps: DemandPartnerMaxQps,
    rulesUpdate: Map[RuleId, DesiredToggledQps],
    modified: ModifiedInstant): Future[ConfigurationError \/ VersionSummary] = {
    client.execTxEither { implicit s =>
      for {
        v <- queries.Versions.get(vId)
          .\/>(ConfigurationError.versionNotFound(vId))
        _ <- if (v.published.nonEmpty) -\/(ConfigurationError.versionAlreadyPublished(v.id))
        else \/-(v.published)
        _ <- ().right[ConfigurationError]
        versionUpdate = queries.normalizeExceptions {
          queries.Versions.setQps(vId, maxQps, modified)
        }
        // If update doesn't return 1,
        // something didn't exist and no update was made
        _ <-
        if (versionUpdate =/= 1) ConfigurationError.versionNotFound(vId).left
        else ().right

        existingSummaries = queries.VersionRules.listSummariesByVersion(vId)
        existing = existingSummaries
          .map(_.id.ruleId).toSet
        _ <- if (!rulesUpdate.keySet.subsetOf(existing))
          ConfigurationError
            .rulesNotInVersion(rulesUpdate.keySet.diff(existing)).left
        else ().right

        _ <- rulesUpdate.toList.traverseU {
          case (rId, qps) =>
            val vrId = VersionRuleId(vId, rId)
            val qpsUpdate = queries.normalizeExceptions {
              queries.VersionRules.setQps(vrId, qps)
            }
            if (qpsUpdate =/= 1) ConfigurationError.ruleNotInVersion(vrId).left
            else ().right
        }
        v <- queries.Versions.get(vId).\/>(
          ConfigurationError.generalError(
            s"Unable to retrieve version: '${vId.value}' after updating it. This should not have happened"))

        result = VersionSummary.replaceQpsState(rulesUpdate).exec(
          VersionSummary.fromVersionAndSummaries(v, existingSummaries))
      } yield result
    }
  }

  def replaceRuleInVersion(
    existingId: VersionRuleId,
    newRule: RuleId): Future[ConfigurationError \/ RuleId] =
    client.execTxEither {
      replaceRuleInVersionWithSession(
        existingId,
        newRule)(_)
    }

  private def replaceRuleInVersionWithSession(
    existingId: VersionRuleId,
    newRule: RuleId)(implicit s: JdbcBackend.Session): ConfigurationError \/ RuleId =
    try {
      queries.normalizeExceptions {
        val updateResult = queries.VersionRules.replaceVersion(
          existingId,
          newRule)
        if (updateResult === 1) newRule.right
        else ConfigurationError.ruleNotInVersion(existingId).left
      }
    } catch {
      case e: UniqueConstraintViolation =>
        ConfigurationError.ruleAlreadyExists(newRule).left
      case e: ForeignKeyViolationError =>
        ConfigurationError.ruleNotFound(existingId.ruleId).left
    }


  def addRuleToVersion(
    versionId: VersionId,
    newRule: RuleId,
    qps: DesiredToggledQps): Future[ConfigurationError \/ RuleId] =
    client.execTxEither {
      addRuleToVersionWithSession(versionId, newRule, qps)(_)
    }

  private def addRuleToVersionWithSession(
    versionId: VersionId,
    newRule: RuleId,
    qps: DesiredToggledQps)(implicit s: JdbcBackend.Session): ConfigurationError \/ RuleId =
    try {
      queries.normalizeExceptions(
        queries.VersionRules
          .insert(VersionRuleId(versionId, newRule), qps))
        .right
        .map(_ => newRule)
    } catch {
      case e: UniqueConstraintViolation =>
        ConfigurationError.ruleAlreadyExists(newRule).left
      case e: ForeignKeyViolationError =>
        e.constraint.some { c =>
          if (c.nameMatches(".*_version_id_.*".r))
            ConfigurationError.versionNotFound(versionId).left
          else
            ConfigurationError.ruleNotFound(newRule).left
        }.none(ConfigurationError.
          generalError("Unknown FK violation attempting to add rule to version").left)
    }

  def removeRuleFromVersion(
    vrId: VersionRuleId): Future[ConfigurationError \/ Unit] =
    client.execTxEither { implicit s =>
      for {
        v <- queries.Versions.get(vrId.versionId)
          .\/>(ConfigurationError.versionNotFound(vrId.versionId))
        _ <- if (v.published.nonEmpty) -\/(ConfigurationError.versionAlreadyPublished(v.id))
        else \/-(v.published)
        _ <- queries.VersionRules.get(vrId)
          .\/>(ConfigurationError.ruleNotInVersion(vrId))
      } yield queries.VersionRules.delete(vrId)(s)
    }

  def copyVersion(
    sourceId: VersionId,
    newId: VersionId,
    created: CreatedInstant): Future[ConfigurationError \/ VersionSummary] =
    client.execTxEither {
      implicit s =>
        for {
          _ <- ().right[ConfigurationError]

          // copy version
          vResult <- try {
            queries.normalizeExceptions(
              queries.Versions.copy(sourceId, newId, created)).right
          } catch {
            case e: UniqueConstraintViolation =>
              ConfigurationError.versionAlreadyExists(newId).left
          }

          _ <-
          if (vResult =/= 1) ConfigurationError.versionNotFound(sourceId).left
          else ().right

          // copy versions_rules
          // If there were UniqueConstraintViolation,
          // wouldn't have gotten this far
          vrResult = queries.normalizeExceptions(
            queries.VersionRules.copy(sourceId, newId))

          result <- queries.Versions.get(newId).map { v =>
            VersionSummary.fromVersionAndSummaries(
              v,
              queries.VersionRules.listSummariesByVersion(newId))
          }.\/>(ConfigurationError.generalError("Version just created was not found"))
        } yield result
    }

  def publishVersion(
    id: VersionId,
    published: PublishedInstant
  ): Future[ConfigurationError \/ VersionSummary] =
    client.execTx { implicit s =>
      for {
        vs <- queries.Versions.get(id).map {
          v =>
            VersionSummary.fromVersionAndSummaries(
              v,
              queries.VersionRules.listSummariesByVersion(id))
        }
          .\/>(ConfigurationError.versionNotFound(id))
        _ <- if (vs.published.nonEmpty) -\/(ConfigurationError.versionAlreadyPublished(vs.id))
        else \/-(vs.published)

      } yield {
        queries.Versions.publish(id, published)

        VersionSummary(
          vs.id,
          vs.routerConfigurationId,
          vs.created,
          vs.modified,
          Some(published),
          vs.maxQps,
          vs.rules)
      }

    }.recover {
      case e: ForeignKeyViolationError =>
        e.constraint.some { c =>
          if (c.nameMatches(".*_version_id_.*".r))
            ConfigurationError.versionNotFound(id).left
          else
            ConfigurationError.versionNotFound(id).left
        }.none(ConfigurationError.
          generalError("Unknown FK violation attempting to publish version").left)
    }

  def updateRuleInVersion(
    vrId: VersionRuleId,
    newRule: Rule): Future[ConfigurationError \/ Rule] =
    for {
      conditions <- SqlRuleConditions.fromRuleConditions(
        newRule.id,
        newRule.conditions,
        newRuleConditionId)
      sqlRule = SqlRule(newRule.id, newRule.name, newRule.created, newRule.trafficType)
      sqlConditions = conditions.values.toNel
      sqlConditionExceptions = conditions.exceptions.toNel
      result <- client.execTxEither { implicit s =>
        for {
          v <- queries.Versions.get(vrId.versionId)
            .\/>(ConfigurationError.versionNotFound(vrId.versionId))
          _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
          _ <- queries.Rules.get(newRule.id).map(a => ConfigurationError.ruleAlreadyExists(newRule.id))
            .<\/(())
          _ <- addRuleWithSession(
            sqlRule,
            sqlConditions,
            sqlConditionExceptions)
          _ <- replaceRuleInVersionWithSession(vrId, newRule.id)
        } yield newRule
      }
    } yield result

  def saveRuleToVersion(
    vId: VersionId,
    newRule: Rule,
    qps: DesiredToggledQps): Future[ConfigurationError \/ Rule] = for {
    conditions <- SqlRuleConditions.fromRuleConditions(
      newRule.id,
      newRule.conditions,
      newRuleConditionId)
    sqlRule = SqlRule(newRule.id, newRule.name, newRule.created, newRule.trafficType)
    sqlConditions = conditions.values.toNel
    sqlConditionExceptions = conditions.exceptions.toNel

    result <- client.execTxEither { implicit s =>
      for {
        v <- queries.Versions.get(vId)
          .\/>(ConfigurationError.versionNotFound(vId))
        _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
        _ <- queries.Rules.get(newRule.id).map(a => ConfigurationError.ruleAlreadyExists(newRule.id))
          .<\/(())
        _ <-
        if (queries.VersionRules.rulesCountForVersion(vId) >= MaximumRulesReached.Max)
          -\/(ConfigurationError.maximumRulesReached(vId))
        else \/-(vId)
        ruleResult <- addRuleWithSession(
          sqlRule,
          sqlConditions,
          sqlConditionExceptions)
        versionResult <- addRuleToVersionWithSession(vId, newRule.id, qps)
      } yield newRule
    }
  } yield result


  def deleteVersion(id: VersionId): Future[Unit] = client.execTx { implicit s =>
    queries.normalizeExceptions(queries.Versions.delete(id))
  }

  def listVersions(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[Version]] = client.exec { implicit s =>
    queries.Versions.list(dp, sp, offset, limit)
  }

  def listVersionSummaries(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[VersionSummary]] =
    client.exec { implicit s =>
      val versions = queries.Versions.list(dp, sp, offset, limit)
      versions.toNel.some {
        nelV =>
          val summaries = queries.VersionRules
            .listSummariesByVersions(nelV.map(_.id).toList)
            .groupBy(_.id.versionId)
          nelV.map {
            v =>
              VersionSummary.fromVersionAndSummaries(
                v,
                summaries.getOrElse(v.id, List.empty))
          }.toList
      }.none(List.empty)
    }

  def listVersionsForRule(
    rId: RuleId,
    dpId: DemandPartnerId,
    spIds: List[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[Version]] = client.exec { implicit s =>
    queries.VersionRules.listVersionsForRule(rId, dpId, spIds, offset, limit)
  }

  def addRouterConfiguration(
    cfg: RouterConfiguration,
    initialVersion: VersionId,
    created: CreatedInstant,
    initialMaxQps: DemandPartnerMaxQps): Future[ConfigurationError \/ RouterConfiguration] =
    client.execTxEither { implicit s =>
      val newVersion =
        Version(initialVersion, cfg.id, created, ModifiedInstant(created.value), None, initialMaxQps)
      for {
        _ <- try {
          queries.normalizeExceptions(queries.RouterConfigurations.insert(cfg.id)).right
        } catch {
          case e: UniqueConstraintViolation =>
            ConfigurationError.routerConfigurationAlreadyExists(cfg.id).left
          case e: ForeignKeyViolationError =>
            e.constraint.some { c =>
              if (c.nameMatches(".*_dp_id_.*".r))
                ConfigurationError.demandPartnerNotFound(cfg.id.dpId).left
              else if (c.nameMatches(".*_sp_id_.*".r))
                ConfigurationError.supplyPartnerNotFound(cfg.id.spId).left
              else
                ConfigurationError.generalError(
                  "Unknown FK violation attempting to add configuration").left
            }.none(ConfigurationError
              .generalError("Unknown FK violation attempting to add configuration").left)
        }
        _ <- queries.normalizeExceptions(
          queries.RouterConfigurationTargets.insertForRouterConfiguration(
            cfg.id,
            cfg.target)).right
        _ <- try {
          queries.normalizeExceptions(queries.Versions.insert(newVersion)).right
        } catch {
          case e: UniqueConstraintViolation =>
            ConfigurationError.versionAlreadyExists(newVersion.id).left
        }
      } yield cfg
    }

  def getRouterConfiguration(id: RouterConfigurationId): Future[Option[RouterConfiguration]] =
    client.exec { implicit s =>
      queries.RouterConfigurations.get(id).map { i =>
        val targets = queries.RouterConfigurationTargets.getByRouterConfiguration(id)
        RouterConfiguration(i, targets.get)
      }
    }

  def getRouterConfigurations(
    dpId: DemandPartnerId): Future[Option[List[RouterConfiguration]]] =
    client.exec { implicit s =>
      // Requirement here is that if DP doesn't exist, return None
      queries.DemandPartners.get(dpId).map { _ =>
        val ids = queries.RouterConfigurations.getByDemandPartnerId(dpId)
        val targets = queries.RouterConfigurationTargets.getByDemandPartnerId(dpId)
        ids.sorted(implicitly[Order[RouterConfigurationId]].toScalaOrdering)
          .flatMap(id => targets.get(id).map(RouterConfiguration(id, _)))
      }
    }

  def deleteRouterConfiguration(id: RouterConfigurationId): Future[Unit] =
    client.execTx { implicit s =>
      queries.normalizeExceptions {
        queries.RouterConfigurationTargets.deleteByRouterConfiguration(id)
        queries.RouterConfigurations.delete(id)
      }
    }

  def getRule(id: RuleId): Future[Option[Rule]] =
    client.exec { implicit s =>
      queries.Rules.get(id).map {
        r =>
          val conditions = queries.RuleConditions.listByRuleId(r.id)
          val exceptions =
            conditions.toNel.some(c =>
              queries.RuleConditionExceptions.listByRuleConditions(c.map(_.id)))
              .none(List.empty)
          Rule(
            r.id,
            r.name,
            r.created,
            r.trafficType,
            SqlRuleConditions.toRuleConditions(
              SqlRuleConditions(
                conditions,
                exceptions)).getOrElse(r.id, RuleConditions.Empty))
      }
    }

  def addRule(rule: Rule): Future[\/[ConfigurationError, Rule]] = for {
    conditions <- SqlRuleConditions.fromRuleConditions(
      rule.id,
      rule.conditions,
      newRuleConditionId)
    sqlRule = SqlRule(rule.id, rule.name, rule.created, rule.trafficType)
    sqlConditions = conditions.values.toNel
    sqlConditionExceptions = conditions.exceptions.toNel

    result <- client.execTx(
      addRuleWithSession(sqlRule, sqlConditions, sqlConditionExceptions)(_))
  } yield result.map(_ => rule)

  private def addRuleWithSession(
    sqlRule: SqlRule,
    sqlConditions: Option[NonEmptyList[SqlRuleCondition]],
    sqlConditionExceptions: Option[NonEmptyList[SqlRuleConditionException]])(
    implicit s: JdbcBackend.Session): ConfigurationError \/ Unit =
    for {
      _ <- try {
        queries.normalizeExceptions {
          queries.Rules.insert(sqlRule).right[ConfigurationError]
        }
      } catch {
        case e: UniqueConstraintViolation =>
          ConfigurationError.ruleAlreadyExists(sqlRule.id).left
      }
      _ <- try {
        queries.normalizeExceptions {
          sqlConditions.traverseU { c =>
            queries.RuleConditions.insert(c).right[ConfigurationError]
          }
        }
      } catch {
        case e: UniqueConstraintViolation =>
          ConfigurationError.generalError("Unable to add rule conditions").left
      }
      _ <- try {
        queries.normalizeExceptions {
          sqlConditionExceptions.traverseU { c =>
            queries.RuleConditionExceptions.insert(c).right[ConfigurationError]
          }
        }
      } catch {
        case e: UniqueConstraintViolation =>
          ConfigurationError.generalError("Unable to add rule condition exceptions").left
      }
    } yield {}

  def deleteRule(id: RuleId): Future[Unit] =
    client.execTx { implicit s =>
      queries.normalizeExceptions {
        queries.RuleConditionExceptions.deleteByRuleId(id)
        queries.RuleConditions.deleteByRuleIds(NonEmptyList(id))
        queries.Rules.delete(id)
        ()
      }
    }

  def getUserList(id: UserListId): Future[Option[UserList]] =
    client.exec { implicit s =>
      queries.UserLists.get(id)
    }

  def getUserListByName(name: UserListName, dpId: DemandPartnerId): Future[Option[UserList]] =
    client.exec { implicit s =>
      queries.UserLists.getByName(name,dpId)
    }

  def listUserLists(
    dpId: Option[DemandPartnerId],
    offset: Offset,
    limit: Limit): Future[List[UserList]] =
    client.exec { implicit s =>
    queries.UserLists.list(dpId, offset, limit)
  }


}

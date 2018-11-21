package com.ntoggle.kubitschek
package domainpersistence
package mem

import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.goldengate.java.RichReadWriteLock.RichReadWriteLock
import com.ntoggle.kubitschek.domain._

import scala.concurrent.Future
import scalaz._
import scalaz.syntax.equal._
import scalaz.syntax.std.option._
import scalaz.syntax.std.boolean._

object MemPersistence {
  def empty: Persistence = new MemPersistence(
    Map.empty,
    Map.empty,
    Map.empty,
    Map.empty,
    Map.empty,
    Map.empty,
    Map.empty)
}

class MemPersistence(
  private var demandPartners: Map[DemandPartnerId, DemandPartner],
  private var supplyPartners: Map[SupplyPartnerId, SupplyPartner],
  private var routerConfigurations: Map[RouterConfigurationId, RouterConfiguration],
  private var versions: Map[VersionId, Version],
  private var versionRules: Map[VersionRuleId, VersionRule],
  private var rules: Map[RuleId, Rule],
  private var userLists: Map[UserListId, UserList])
  extends Persistence {
  private val l = new ReentrantReadWriteLock()

  def addDemandPartner(
    dp: DemandPartner): Future[ConfigurationError \/ DemandPartner] = {
    val result = l.withWriteLock {
      for {
        _ <- demandPartners.get(dp.id)
          .map(a => ConfigurationError.demandPartnerAlreadyExists(a.id))
          .<\/(())
        _ <- demandPartners.values.find(_.name === dp.name)
          .map(a => ConfigurationError.demandPartnerNameAlreadyExists(a.name))
          .<\/(())
      } yield {
        demandPartners = demandPartners + (dp.id -> dp)
        dp
      }
    }
    Future.successful(result)
  }

  def deleteDemandPartner(id: DemandPartnerId): Future[Unit] = ???

  def setDemandPartner(dp: DemandPartner): Future[ConfigurationError \/ DemandPartner] = {
    val result = l.withWriteLock {
      for {
        _ <- demandPartners.get(dp.id)
          .\/>(ConfigurationError.demandPartnerNotFound(dp.id))
      // validate that user may not change certain fields
      } yield {
        demandPartners = demandPartners + (dp.id -> dp)
        dp
      }
    }
    Future.successful(result)
  }

  def getDemandPartner(
    id: DemandPartnerId): Future[Option[DemandPartner]] = {
    val dp = l.withReadLock {
      demandPartners.get(id)
    }
    Future.successful(dp)
  }

  def listDemandPartners(
    offset: Offset,
    limit: Limit): Future[List[DemandPartner]] = {
    val result = applyOffsetLimit(
      l.withReadLock(demandPartners.values.toList),
      offset,
      limit)(_.id)
    Future.successful(result)
  }

  def addSupplyPartner(
    sp: SupplyPartner): Future[ConfigurationError \/ SupplyPartner] = {
    val result = l.withWriteLock {
      for {
        _ <- supplyPartners.get(sp.id)
          .map(a => ConfigurationError.supplyPartnerAlreadyExists(a.id))
          .<\/(())
        _ <- supplyPartners.values.find(_.name === sp.name)
          .map(a => ConfigurationError.supplyPartnerNameAlreadyExists(a.name))
          .<\/(())
      } yield {
        supplyPartners = supplyPartners + (sp.id -> sp)
        sp
      }
    }
    Future.successful(result)
  }

  def setSupplyPartner(sp: SupplyPartner): Future[ConfigurationError \/ SupplyPartner] = {
    val result = l.withWriteLock {
      for {
        _ <- supplyPartners.get(sp.id)
          .\/>(ConfigurationError.supplyPartnerNotFound(sp.id))
      // validate that user may not change certain fields
      } yield {
        supplyPartners = supplyPartners + (sp.id -> sp)
        sp
      }
    }
    Future.successful(result)
  }

  def deleteSupplyPartner(id: SupplyPartnerId): Future[Unit] = ???

  def getSupplyPartner(
    id: SupplyPartnerId): Future[Option[SupplyPartner]] = {
    val sp = l.withReadLock(supplyPartners.get(id))
    Future.successful(sp)
  }

  def listSupplyPartners(
    offset: Offset,
    limit: Limit): Future[List[SupplyPartner]] = {
    val sp = applyOffsetLimit(
      l.withReadLock(supplyPartners.values.toList),
      offset,
      limit)(_.id)
    Future.successful(sp)
  }

  def addRouterConfiguration(
    cfg: RouterConfiguration,
    initialVersion: VersionId,
    created: CreatedInstant,
    initialMaxQps: DemandPartnerMaxQps): Future[ConfigurationError \/ RouterConfiguration] = {
    val result = l.withWriteLock {
      val newConfig = for {
        _ <- validateDemandPartnerSupplyPartnerType(
          cfg.id.dpId,
          cfg.id.spId)
      } yield cfg
      newConfig.foreach(c => {
        versions = versions + (initialVersion -> Version(
          initialVersion,
          c.id,
          created,
          ModifiedInstant(created.value),
          None,
          initialMaxQps))
        routerConfigurations = routerConfigurations + (c.id -> c)
      })
      newConfig
    }
    Future.successful(result)
  }

  def getRouterConfiguration(
    id: RouterConfigurationId): Future[Option[RouterConfiguration]] = {
    val result = l.withReadLock(routerConfigurations.get(id))
    Future.successful(result)
  }

  def getRouterConfigurations(
    dpId: DemandPartnerId): Future[Option[List[RouterConfiguration]]] = {

    val result = l.withReadLock {
      demandPartners.get(dpId).map(_ =>
        for {
          (rcId, rc) <- routerConfigurations.toList
          if rcId.dpId === dpId
        } yield rc
      )
    }
    Future.successful(result)
  }

  def deleteRouterConfiguration(id: RouterConfigurationId): Future[Unit] =
    Future.successful(())

  def addVersion(
    id: VersionId,
    cfgId: RouterConfigurationId,
    created: CreatedInstant,
    maxQps: DemandPartnerMaxQps): Future[ConfigurationError \/ Version] = {
    val result = l.withWriteLock {
      val version = for {
        _ <- versions.get(id)
          .map(_ => ConfigurationError.versionAlreadyExists(id)).<\/(())
        _ <- validateDemandPartnerSupplyPartnerType(
          cfgId.dpId,
          cfgId.spId)
      } yield Version(id, cfgId, created, ModifiedInstant(created.value), None, maxQps)

      version.foreach {
        v => {
          versions = versions + (v.id -> v)
        }
      }
      version
    }
    Future.successful(result)
  }

  private def filterV(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): List[Version] = applyOffsetLimit(
    versions.values.toList.filter { v =>
      dp.some(_ === v.routerConfigurationId.dpId).none(true) &&
        sp.some(_ === v.routerConfigurationId.spId).none(true)
    },
    offset,
    limit)(_.created)

  def copyVersion(
    id: VersionId,
    newId: VersionId,
    created: CreatedInstant
  ): Future[ConfigurationError \/ VersionSummary] = {

    val result = for {
      vs <- getVS(id)
        .\/>(ConfigurationError.versionNotFound(id))
    } yield VersionSummary(
      newId,
      vs.routerConfigurationId,
      created,
      ModifiedInstant(created.value),
      None,
      vs.maxQps,
      vs.rules
    )

    result.foreach(vs => {
      val v = Version(vs.id, vs.routerConfigurationId, vs.created, vs.modified, vs.published, vs.maxQps)
      versions = versions + (vs.id -> v)
      // make a new rule <-> version mapping

      vs.rules.foreach(r => {
        val vri = VersionRuleId(vs.id, r.id.ruleId)
        versionRules = versionRules + (vri -> VersionRule(vri, r.desiredQps))
      })

    })

    Future.successful(result)
  }

  def publishVersion(
    id: VersionId,
    published: PublishedInstant
  ): Future[ConfigurationError \/ VersionSummary] = {

    // we do not make a copy to be returned.

    val result = for {
      vs <- getVS(id)
        .\/>(ConfigurationError.versionNotFound(id))
      _ <- if (vs.published.nonEmpty) -\/(ConfigurationError.versionAlreadyPublished(vs.id))
      else \/-(published)

    } yield VersionSummary(
      vs.id,
      vs.routerConfigurationId,
      vs.created,
      vs.modified,
      Some(published),
      vs.maxQps,
      vs.rules)

    result.foreach(vs => {
      val v = Version(vs.id, vs.routerConfigurationId, vs.created, vs.modified, vs.published, vs.maxQps)
      versions = versions + (id -> v)
    })

    Future.successful(result)
  }

  def listVersions(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[Version]] = {
    val result = l.withReadLock(filterV(dp, sp, offset, limit))
    Future.successful(result)
  }

  def listVersionSummaries(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[VersionSummary]] = {
    val result = l.withReadLock {
      for {
        v <- filterV(dp, sp, offset, limit)
        vs <- getVS(v.id)
      } yield vs
    }
    Future.successful(result)
  }

  def getVersion(
    id: VersionId): Future[Option[Version]] = {
    val result = l.withReadLock(versions.get(id))
    Future.successful(result)
  }

  private def getVS(id: VersionId): Option[VersionSummary] = for {
    v <- versions.get(id)
    vrIdVr = versionRules.filterKeys(_.versionId === id).toList
    vrs = for {
      (vrId, vr) <- vrIdVr
      r <- rules.get(vrId.ruleId).toList
    } yield VersionRuleSummary(vrId, r.trafficType, vr.desiredQps, r.created, r.name)
  } yield VersionSummary(
    v.id,
    v.routerConfigurationId,
    v.created,
    v.modified,
    v.published,
    v.maxQps,
    vrs)

  def getVersionSummary(id: VersionId): Future[Option[VersionSummary]] = {
    val result = l.withReadLock(getVS(id))
    Future.successful(result)
  }

  def setVersionQps(
    id: VersionId,
    maxQps: DemandPartnerMaxQps,
    rulesUpdate: Map[RuleId, DesiredToggledQps],
    modified: ModifiedInstant): Future[ConfigurationError \/ VersionSummary] = {

    val result = l.withWriteLock {
      for {
        currentVs <- getVS(id)
          .\/>(ConfigurationError.versionNotFound(id))
        _ <- currentVs.published.map(_ => ConfigurationError.versionAlreadyPublished(currentVs.id)).<\/(())
        notFound = rulesUpdate.keySet diff currentVs.rules.map(_.id.ruleId).toSet
        _ <- notFound.nonEmpty
          .option(ConfigurationError.rulesNotInVersion(notFound))
          .<\/(false)
        newVs = (for {
          - <- VersionSummary.maxQpsLens := maxQps
          _ <- VersionSummary.replaceQpsState(rulesUpdate)
        } yield {}).exec(currentVs)
      } yield {
        versions = versions + (
          id -> Version(
            id,
            newVs.routerConfigurationId,
            newVs.created,
            modified,
            newVs.published,
            newVs.maxQps))
        versionRules = versionRules ++
          newVs.rules.map(vr => vr.id -> VersionRule(vr.id, vr.desiredQps))
        newVs
      }
    }
    Future.successful(result)
  }

  def replaceRuleInVersion(
    vrId: VersionRuleId,
    rId: RuleId): Future[ConfigurationError \/ RuleId] = {
    val result = l.withWriteLock {
      for {
        v <- versions.get(vrId.versionId)
          .\/>(ConfigurationError.versionNotFound(vrId.versionId))
        _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
        vrOld <- versionRules.get(vrId).\/>(ConfigurationError.generalError("Version with existing rule does not exist"))
        _ <- rules.get(rId)
          .\/>(ConfigurationError.ruleNotFound(vrId.ruleId))
        newVrId = VersionRuleId(vrId.versionId, rId)
      } yield {
        versionRules = versionRules - vrId + (newVrId -> VersionRule(newVrId, vrOld.desiredQps))
        rId
      }
    }
    Future.successful(result)
  }

  def updateRuleInVersion(
    vrId: VersionRuleId,
    newRule: Rule): Future[ConfigurationError \/ Rule] = {
    val result = l.withWriteLock {
      for {
        v <- versions.get(vrId.versionId)
          .\/>(ConfigurationError.versionNotFound(vrId.versionId))
        _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
        vrOld <- versionRules.get(vrId).\/>(ConfigurationError.generalError("Version with existing rule does not exist"))
        _ <- rules.get(newRule.id).map(a => ConfigurationError.ruleAlreadyExists(newRule.id))
          .<\/(())
        newVrId = VersionRuleId(vrId.versionId, newRule.id)
      } yield {
        rules = rules + (newRule.id -> newRule)
        versionRules = versionRules - vrId + (newVrId -> VersionRule(newVrId, vrOld.desiredQps))
        newRule
      }
    }
    Future.successful(result)
  }
  def saveRuleToVersion(
    vId: VersionId,
    newRule: Rule,
    qps: DesiredToggledQps): Future[ConfigurationError \/ Rule] = {
    val result = l.withWriteLock {
      for {
        v <- versions.get(vId)
          .\/>(ConfigurationError.versionNotFound(vId))
        _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
        _ <- rules.get(newRule.id).map(a => ConfigurationError.ruleAlreadyExists(newRule.id))
          .<\/(())
        _ <- if (versionRules.filterKeys(_.versionId == vId).toList.length > 64) -\/(ConfigurationError.maximumRulesReached(vId))
        else \/-(vId)
        newVrId = VersionRuleId(vId, newRule.id)
      } yield {
        rules = rules + (newRule.id -> newRule)
        versionRules = versionRules + (newVrId -> VersionRule(newVrId, qps))
        newRule
      }
    }
    Future.successful(result)
  }

  def addRuleToVersion(
    versionId: VersionId,
    rId: RuleId,
    qps: DesiredToggledQps
  ): Future[ConfigurationError \/ RuleId] = {
    val result = l.withWriteLock {
      for {
        v <- versions.get(versionId)
          .\/>(ConfigurationError.versionNotFound(versionId))
        _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
        _ <- rules.get(rId)
          .\/>(ConfigurationError.ruleNotFound(rId))
        _ <- if (versionRules.filterKeys(_.versionId == versionId).toList.length > 64) -\/(ConfigurationError.maximumRulesReached(versionId))
        else \/-(versionId)
        vr = VersionRuleId(versionId, rId)
      } yield {
        versionRules = versionRules + (vr -> VersionRule(vr, qps))
        rId
      }
    }
    Future.successful(result)
  }

  def removeRuleFromVersion(
    vrId: VersionRuleId): Future[ConfigurationError \/ Unit] = {
    val result = l.withWriteLock {
      //          TODO BETA-716
      for {
        v <- versions.get(vrId.versionId)
          .\/>(ConfigurationError.versionNotFound(vrId.versionId))
        _ <- rules.get(vrId.ruleId)
          .\/>(ConfigurationError.ruleNotFound(vrId.ruleId))
        _ <- v.published.map(_ => ConfigurationError.versionAlreadyPublished(v.id)).<\/(())
        _ <- versionRules.get(vrId).\/>(ConfigurationError.ruleNotInVersion(vrId))
      } yield
        versionRules = versionRules - vrId
    }
    Future.successful(result)
  }

  def deleteVersion(id: VersionId): Future[Unit] =
    Future.successful(())

  def getRule(id: RuleId): Future[Option[Rule]] = {
    val result = l.withReadLock(rules.get(id))
    Future.successful(result)
  }

  def addRule(rule: Rule): Future[ConfigurationError \/ Rule] = {
    val result = l.withWriteLock {
      (!rules.contains(rule.id)).option({
        rules = rules + (rule.id -> rule)
        rule
      }).toRightDisjunction(ConfigurationError.ruleAlreadyExists(rule.id))
    }
    Future.successful(result)
  }

  def listVersionsForRule(rId: RuleId,
    dpId: DemandPartnerId,
    spIds: List[SupplyPartnerId],
    offset: Offset,
    limit: Limit) : Future[List[Version]] = {

    val vl = for {
      vr <- versionRules.toList.filter { _._1.ruleId == rId }
      v <- versions.get(vr._1.versionId) if v.routerConfigurationId.dpId == dpId && spIds.contains(v.routerConfigurationId.spId)

    } yield v

    Future.successful(applyOffsetLimit(vl, offset, limit)(_.created))
  }

  def getUserList(id: UserListId) : Future[Option[UserList]] = {
    val result = l.withReadLock(userLists.get(id))
    Future.successful(result)
  }

  def getUserListByName(name: UserListName, dpId: DemandPartnerId) : Future[Option[UserList]] = {
    val ul = userLists.values.toList.find( u => u.name == name && u.dpId == dpId )
    Future.successful(ul)
  }

  def listUserLists(
    dp: Option[DemandPartnerId],
    offset: Offset,
    limit: Limit): Future[List[UserList]] = {

    val items = for {
      dpId <- dp.toList
      ul <- userLists.toList.filter { _._2.dpId == dpId }
    } yield ul._2
    Future.successful(applyOffsetLimit(items.toList, offset, limit)(_.name))
  }

  private def published(
    v: Version
  ): ConfigurationError \/ (Version) =
    for {
      _ <- if (v.published.contains(Some)) \/-(v.published)
      else -\/(ConfigurationError.versionAlreadyPublished(v.id))
    } yield v

  private def validateDemandPartnerSupplyPartnerType(
    dpId: DemandPartnerId,
    spId: SupplyPartnerId): ConfigurationError \/ (DemandPartner, SupplyPartner) =
    for {
      dp <- demandPartners.get(dpId)
        .\/>(ConfigurationError.demandPartnerNotFound(dpId))
      sp <- supplyPartners.get(spId)
        .\/>(ConfigurationError.supplyPartnerNotFound(spId))
    } yield (dp, sp)

  private def applyOffsetLimit[A, B: Order](
    values: List[A],
    o: Offset,
    l: Limit)(f: A => B): List[A] =
    values.sortBy(f)(Order[B].toScalaOrdering)
      .slice(o.value, o.value + l.value)


}

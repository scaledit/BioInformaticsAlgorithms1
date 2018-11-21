package com.ntoggle.kubitschek.domainpersistence

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.kubitschek.domain._

import scala.concurrent.Future
import scalaz.\/

trait Persistence {

  def addDemandPartner(dp: DemandPartner): Future[ConfigurationError \/ DemandPartner]
  def setDemandPartner(dp: DemandPartner): Future[ConfigurationError \/ DemandPartner]
  def getDemandPartner(id: DemandPartnerId): Future[Option[DemandPartner]]
  def listDemandPartners(offset: Offset, limit: Limit): Future[List[DemandPartner]]
  def deleteDemandPartner(id: DemandPartnerId): Future[Unit]

  def addSupplyPartner(sp: SupplyPartner): Future[ConfigurationError \/ SupplyPartner]
  def setSupplyPartner(sp: SupplyPartner): Future[ConfigurationError \/ SupplyPartner]
  def getSupplyPartner(id: SupplyPartnerId): Future[Option[SupplyPartner]]
  def listSupplyPartners(offset: Offset, limit: Limit): Future[List[SupplyPartner]]
  def deleteSupplyPartner(id: SupplyPartnerId): Future[Unit]

  def addVersion(
    id: VersionId,
    cfgId: RouterConfigurationId,
    created: CreatedInstant,
    maxQps: DemandPartnerMaxQps): Future[ConfigurationError \/ Version]

  def listVersions(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[Version]]

  def listVersionSummaries(
    dp: Option[DemandPartnerId],
    sp: Option[SupplyPartnerId],
    offset: Offset,
    limit: Limit): Future[List[VersionSummary]]

  def copyVersion(
    id: VersionId,
    newId: VersionId,
    created: CreatedInstant
    ): Future[ConfigurationError \/ VersionSummary]

  def publishVersion(
    id: VersionId,
    published: PublishedInstant
    ): Future[ConfigurationError \/ VersionSummary]

  def getVersion(id: VersionId): Future[Option[Version]]
  def getVersionSummary(id: VersionId): Future[Option[VersionSummary]]
  def setVersionQps(
    id: VersionId,
    maxQps: DemandPartnerMaxQps,
    rules: Map[RuleId, DesiredToggledQps],
    modified: ModifiedInstant): Future[ConfigurationError \/ VersionSummary]
  def replaceRuleInVersion(existingId: VersionRuleId, newRule: RuleId): Future[ConfigurationError \/ RuleId]
  def saveRuleToVersion(versionId: VersionId, newRule: Rule, qps: DesiredToggledQps): Future[ConfigurationError \/ Rule]
  def updateRuleInVersion(existingId: VersionRuleId, newRule: Rule): Future[ConfigurationError \/ Rule]
  def addRuleToVersion(
    versionId: VersionId,
    newRule: RuleId,
    qps: DesiredToggledQps): Future[ConfigurationError \/ RuleId]
  def removeRuleFromVersion(existingId: VersionRuleId): Future[ConfigurationError \/ Unit]

  def listVersionsForRule(rId: RuleId,
    dpId: DemandPartnerId,
    spIds: List[SupplyPartnerId],
    offset: Offset,
    limit: Limit) : Future[List[Version]]

  def deleteVersion(id: VersionId): Future[Unit]

  def addRouterConfiguration(
    cfg: RouterConfiguration,
    initialVersion: VersionId,
    created: CreatedInstant,
    initialMaxQps: DemandPartnerMaxQps): Future[ConfigurationError \/ RouterConfiguration]
  def getRouterConfiguration(id: RouterConfigurationId): Future[Option[RouterConfiguration]]
  def getRouterConfigurations(id: DemandPartnerId): Future[Option[List[RouterConfiguration]]]
  def deleteRouterConfiguration(id: RouterConfigurationId): Future[Unit]

  def addRule(rule: Rule): Future[ConfigurationError \/ Rule]
  def getRule(id: RuleId): Future[Option[Rule]]

  def getUserList(id: UserListId) : Future[Option[UserList]]
  def getUserListByName(name: UserListName, dpId: DemandPartnerId) : Future[Option[UserList]]

  def listUserLists(
    dp: Option[DemandPartnerId],
    offset: Offset,
    limit: Limit): Future[List[UserList]]
}

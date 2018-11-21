package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.users.UserListId
import com.ntoggle.albi.{SupplyPartnerName, DemandPartnerName, DemandPartnerId, SupplyPartnerId}
import scalaz.{Show, Equal}

sealed trait ConfigurationError
object ConfigurationError {
  type \/[A] = scalaz.\/[ConfigurationError, A]

  def versionAlreadyPublished(
    id: VersionId): ConfigurationError =
    VersionAlreadyPublished(id)

  def versionNotFound(
    id: VersionId): ConfigurationError =
    VersionNotFound(id)

  def versionAlreadyExists(
    id: VersionId): ConfigurationError =
    VersionAlreadyExists(id)

  def supplyPartnerNotFound(
    id: SupplyPartnerId): ConfigurationError =
    SupplyPartnerNotFound(id)

  def supplyPartnerAlreadyExists(id: SupplyPartnerId): ConfigurationError =
    SupplyPartnerAlreadyExists(id)

  def supplyPartnerNameAlreadyExists(name: SupplyPartnerName): ConfigurationError =
    SupplyPartnerNameAlreadyExists(name)

  def demandPartnerNotFound(id: DemandPartnerId): ConfigurationError =
    DemandPartnerNotFound(id)

  def demandPartnerAlreadyExists(id: DemandPartnerId): ConfigurationError =
    DemandPartnerAlreadyExists(id)

  def demandPartnerNameAlreadyExists(
    name: DemandPartnerName): ConfigurationError =
    DemandPartnerNameAlreadyExists(name)

  def routerConfigurationNotFound(
    id: RouterConfigurationId): ConfigurationError =
    RouterConfigurationNotFound(id)

  def ruleAlreadyExists(id: RuleId): ConfigurationError =
    RuleAlreadyExists(id)

  def ruleNotFound(id: RuleId): ConfigurationError =
    RuleNotFound(id)

  def rulesNotFound(l: Set[RuleId]): ConfigurationError =
    RulesNotFound(l)

  def ruleNotInVersion(id: VersionRuleId): ConfigurationError =
    RuleNotInVersion(id)

  def rulesNotInVersion(l: Set[RuleId]): ConfigurationError =
    RulesNotInVersion(l)

  def resourceNotAuthorized(): ConfigurationError =
    ResourceNotAuthorized()

  def maximumRulesReached(
    id: VersionId): ConfigurationError =
    MaximumRulesReached(id)

  def routerConfigurationAlreadyExists(
    id: RouterConfigurationId): ConfigurationError =
    RouterConfigurationAlreadyExists(id)

  def userListNotFound(id: UserListId): ConfigurationError =
    UserListIdNotFound(id)

  def userListNotFoundByName(name: UserListName): ConfigurationError =
    UserListNameNotFound(name)

  def generalError(msg: String): ConfigurationError = GeneralError(msg)

  implicit val equalConfigurationError: Equal[ConfigurationError] = Equal.equalA
  implicit val showConfigurationError: Show[ConfigurationError] = Show.showFromToString
}

case class VersionAlreadyPublished(id: VersionId)
  extends ConfigurationError

case class VersionNotFound(id: VersionId)
  extends ConfigurationError

case class VersionAlreadyExists(id: VersionId)
  extends ConfigurationError

case class SupplyPartnerNotFound(id: SupplyPartnerId)
  extends ConfigurationError

case class SupplyPartnerAlreadyExists(id: SupplyPartnerId)
  extends ConfigurationError

case class SupplyPartnerNameAlreadyExists(name: SupplyPartnerName)
  extends ConfigurationError

case class DemandPartnerNotFound(id: DemandPartnerId)
  extends ConfigurationError

case class DemandPartnerAlreadyExists(id: DemandPartnerId)
  extends ConfigurationError

case class DemandPartnerNameAlreadyExists(id: DemandPartnerName)
  extends ConfigurationError

case class RouterConfigurationNotFound(id: RouterConfigurationId)
  extends ConfigurationError

case class RuleAlreadyExists(id: RuleId) extends ConfigurationError

case class RuleNotFound(id: RuleId) extends ConfigurationError

case class RulesNotFound(l: Set[RuleId]) extends ConfigurationError

case class RuleNotInVersion(id: VersionRuleId) extends ConfigurationError

case class RulesNotInVersion(l: Set[RuleId]) extends ConfigurationError

case class ResourceNotAuthorized() extends ConfigurationError

case class UserListIdNotFound(id: UserListId)
  extends ConfigurationError

case class UserListNameNotFound(name: UserListName)
  extends ConfigurationError

case class MaximumRulesReached(id: VersionId)
  extends ConfigurationError

object MaximumRulesReached {
  val Max = 64
}

case class RouterConfigurationAlreadyExists(id: RouterConfigurationId)
  extends ConfigurationError


case class GeneralError(msg: String) extends ConfigurationError

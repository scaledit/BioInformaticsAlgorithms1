package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.kubitschek.domain._
import com.typesafe.scalalogging.slf4j.LazyLogging
import scala.slick.jdbc.{StaticQuery, JdbcBackend}
import scala.slick.jdbc.StaticQuery._
import scalaz.{State, ISet, NonEmptyList}
import scalaz.std.option._
import scalaz.std.list._
import scalaz.syntax.traverse1._
import scalaz.syntax.std.option._
import scalaz.syntax.std.list._

/**
 * This is hardcoded against Postgresql because we need to call stored procs and
 * pass arrays as parameters to the stored procs
 */
private[sql] object SqlQueries
  extends PostgresSlickDriver
  with SlickMappers
  with PostgresSqlExceptionNormalizing
  with LazyLogging {

  object SupplyPartners {
    def insert(partner: SupplyPartner)(implicit s: JdbcBackend.Session) =
      (u +
        "insert into supply_partners (id, name) values (" +?
        partner.id + ", " +?
        partner.name + ")").execute

    def update(sp: SupplyPartner)(implicit s: JdbcBackend.Session): Int =
      (u +
        "update supply_partners set name = " +? sp.name + " where id = " +? sp.id).first

    def get(id: SupplyPartnerId)(implicit s: JdbcBackend.Session) =
      (queryNA[SupplyPartner]("") +
        "select id, name from supply_partners where id =" +? id).firstOption

    def list(limit: Limit, offset: Offset)(implicit s: JdbcBackend.Session) =
      (queryNA[SupplyPartner]("") +
        "select id, name from supply_partners order by name limit " +? limit + " offset " +? offset).list

    def delete(id: SupplyPartnerId)(implicit s: JdbcBackend.Session) =
      (u + "delete from supply_partners where id = " +? id).execute
  }

  object DemandPartners {
    def insert(partner: DemandPartner)(implicit s: JdbcBackend.Session) =
      (u +
        "insert into demand_partners (id, name) values (" +?
        partner.id + ", " +?
        partner.name + ")").execute

    def update(partner: DemandPartner)(implicit s: JdbcBackend.Session): Int =
      (u +
        "update demand_partners set name = " +? partner.name + " where id = " +? partner.id).first

    def get(id: DemandPartnerId)(implicit s: JdbcBackend.Session) =
      (queryNA[DemandPartner]("") +
        "select id, name from demand_partners where id =" +? id).firstOption

    def list(limit: Limit, offset: Offset)(implicit s: JdbcBackend.Session) =
      (queryNA[DemandPartner]("") +
        "select id, name from demand_partners order by name limit " +? limit + " offset " +? offset).list

    def delete(id: DemandPartnerId)(implicit s: JdbcBackend.Session) =
      (u + "delete from demand_partners where id = " +? id).execute
  }

  object RouterConfigurations {
    def insert(id: RouterConfigurationId)(implicit s: JdbcBackend.Session) =
      (u +
        "insert into router_configurations (dp_id, sp_id) values (" +?
        id.dpId + ", " +?
        id.spId + ")").execute

    def get(id: RouterConfigurationId)(implicit s: JdbcBackend.Session) =
      (queryNA[RouterConfigurationId]("") +
        "select dp_id, sp_id from router_configurations where " +
        "dp_id = " +? id.dpId + " and " +
        "sp_id = " +? id.spId).firstOption

    def getByDemandPartnerId(
      dpId: DemandPartnerId)(implicit s: JdbcBackend.Session): List[RouterConfigurationId] =
      (queryNA[RouterConfigurationId]("") +
        "select dp_id, sp_id from router_configurations where " +
        "dp_id = " +? dpId).list

    def delete(id: RouterConfigurationId)(implicit s: JdbcBackend.Session) =
      (u + "delete from router_configurations where " +
        "dp_id = " +? id.dpId + " and " +
        "sp_id = " +? id.spId).execute
  }

  object RouterConfigurationTargets {
    class EndpointValueAppender(update: StaticQuery[Unit, Int]) {
      def endpointValue(id: RouterConfigurationId, t: ConfigurationEndpoint) =
        update + "(" +?
          id.dpId + ", " +?
          id.spId + ", " +?
          t.host + ", " +?
          t.port.value + ")"
    }

    def insertForRouterConfiguration(
      id: RouterConfigurationId,
      target: ConfigurationEndpoint)(implicit s: JdbcBackend.Session) = {

      (u +
        "insert into router_configuration_endpoints (dp_id, sp_id, host, port) " +
        "values (" +?
        id.dpId + ", " +?
        id.spId + ", " +?
        target.host + ", " +?
        target.port.value + ") "
        ).execute
    }

    def getByRouterConfiguration(
      id: RouterConfigurationId)(implicit s: JdbcBackend.Session) =
      (queryNA[ConfigurationEndpoint]("") +
        "select host, port from router_configuration_endpoints where " +
        "dp_id = " +? id.dpId + " and " +
        "sp_id = " +? id.spId).firstOption

    def getByDemandPartnerId(
      dpId: DemandPartnerId)(implicit s: JdbcBackend.Session): Map[RouterConfigurationId, ConfigurationEndpoint] =
      (queryNA[(RouterConfigurationId, ConfigurationEndpoint)]("") +
        "select dp_id, sp_id, host, port from router_configuration_endpoints where " +
        "dp_id = " +? dpId).toMap

    def deleteByRouterConfiguration(
      id: RouterConfigurationId)(implicit s: JdbcBackend.Session) =
      (u + "delete from router_configuration_endpoints where " +
        "dp_id = " +? id.dpId + " and " +
        "sp_id = " +? id.spId).execute
  }

  object Versions {

    def insert(version: Version)(implicit s: JdbcBackend.Session) =
      (u +
        "insert into versions (id, dp_id, sp_id, created, modified, published, max_qps) values (" +?
        version.id + ", " +?
        version.routerConfigurationId.dpId + ", " +?
        version.routerConfigurationId.spId + ", " +?
        version.created + ", " +?
        version.modified + ", " +?
        version.published + ", " +?
        version.maxQps + ")").execute

    def copy(
      sourceId: VersionId,
      newId: VersionId,
      created: CreatedInstant)(implicit s: JdbcBackend.Session) =
      (u + "insert into versions (id, dp_id, sp_id, created, modified, published, max_qps) (" +
        "select " +? newId + ", " +
        "dp_id, sp_id, " +? created + ", " +? created + ", " +
        "NULL, max_qps from versions where id = " +? sourceId + ")").first

    def get(id: VersionId)(implicit s: JdbcBackend.Session) =
      (queryNA[Version]("") +
        "select id, dp_id, sp_id, created, modified, published, max_qps " +
        "from versions where id = " +? id).firstOption

    def publish(
      vId: VersionId,
      published: PublishedInstant)(implicit s: JdbcBackend.Session): Int = {
      (u + "update versions set published = " +? Some(published) +
        " where id = " +? vId).first
    }

    def setQps(
      vId: VersionId,
      qps: DemandPartnerMaxQps,
      modified: ModifiedInstant)(implicit s: JdbcBackend.Session) = {
      (u + "update versions set max_qps = " +? qps +
        ", modified = " +? modified +
        " where id = " +? vId).first
    }

    def delete(id: VersionId)(implicit s: JdbcBackend.Session) =
      (u + "delete from versions where id = " +? id).execute

    def deleteByRouterConfiguration(
      id: RouterConfigurationId)(implicit s: JdbcBackend.Session) =
      (u + "delete from versions where " +
        "dp_id = " +? id.dpId + " and " +
        "sp_id = " +? id.spId).execute

    def list(
      dp: Option[DemandPartnerId],
      sp: Option[SupplyPartnerId],
      offset: Offset,
      limit: Limit)(implicit s: JdbcBackend.Session): List[Version] = {
      type S = StaticQuery[Unit, Version]
      val whereDp = dp.map(id => State.modify[S](_ + " dp_id = " +? id))
      val whereSp = sp.map(id => State.modify[S](_ + " sp_id = " +? id))
      val and = State.modify[S](_ + " and ")
      val wheres = List(whereDp.toList, whereSp.toList).flatten.toNel
        .some { w =>
        State.modify[S](_ + " where ") :: w.head :: w.tail.flatMap(and :: _ :: Nil)
      }.none(Nil)
      (for {
        _ <- State.modify[S](_ + "select id, dp_id, sp_id, created, modified, published, max_qps from versions")
        _ <- wheres.sequence[({type l[a] = State[S, a]})#l, Unit]
        _ <- State.modify[S](
          _ + " order by published DESC,modified DESC,created DESC limit " +? limit + " offset " +? offset)
      } yield ()).exec(queryNA[Version]("")).list
    }
  }

  object VersionRules {

    def rulesCountForVersion(id: VersionId)(implicit s: JdbcBackend.Session) =
      (queryNA[Int]("") +
        "select count(*) from versions_rules where version_id = " +? id).first

    def copy(
      sourceId: VersionId,
      newId: VersionId)(implicit s: JdbcBackend.Session): Int =
      (u + "insert into versions_rules (rule_id, version_id, desired_qps) " +
        "(select rule_id, " +? newId + ", desired_qps " +
        "from versions_rules where version_id = " +? sourceId + ")").first

    def get(vrId: VersionRuleId)(implicit s: JdbcBackend.Session) =
      (queryNA[VersionRule]("") +
        "select version_id, rule_id, desired_qps from versions_rules" +
        " where rule_id = " +? vrId.ruleId +
        " and version_id = " +? vrId.versionId).firstOption

    def insert(id: VersionRuleId, qps: DesiredToggledQps)(implicit s: JdbcBackend.Session) =
      (u + "insert into versions_rules (rule_id, version_id, desired_qps) values (" +?
        id.ruleId + ", " +?
        id.versionId + ", " +?
        qps + ")").execute

    def replaceVersion(
      existing: VersionRuleId,
      newRule: RuleId)(implicit s: JdbcBackend.Session): Int = {
      (u + "update versions_rules set rule_id = " +? newRule +
        " where rule_id = " +? existing.ruleId +
        " and version_id = " +? existing.versionId).first
    }

    def setQps(
      existing: VersionRuleId,
      qps: DesiredToggledQps)(implicit s: JdbcBackend.Session): Int = {
      (u + "update versions_rules set desired_qps = " +? qps +
        " where rule_id = " +? existing.ruleId +
        " and version_id = " +? existing.versionId).first
    }

    def listSummariesByVersion(
      id: VersionId)(implicit s: JdbcBackend.Session): List[VersionRuleSummary] = {
      (queryNA[VersionRuleSummary]("") +
        "select version_id, rule_id, traffic_type, desired_qps, created, name from versions_rules inner join rules on rule_id = id where version_id = " +? id + " order by created DESC").list
    }

    def listSummariesByVersions(
      ids: List[VersionId])(implicit s: JdbcBackend.Session): List[VersionRuleSummary] = {
      def whereVid[A](
        id: VersionId,
        q: StaticQuery[Unit, A]) = q + " version_id = " +? id

      val initial = queryNA[VersionRuleSummary]("") +
        "select version_id, rule_id, traffic_type, desired_qps, created, name from versions_rules inner join rules on rule_id = id"
      ids.toNel.some { nelIds =>
        // first part of where clause is special as it doesn't start with 'and'
        nelIds.foldLeft(whereVid(nelIds.head, initial + " where")) {
          (acc, id) => whereVid(id, acc + " or ")
        }
      }.none(initial).list
    }

    def listVersionsForRule(
      id: RuleId,
      dpId: DemandPartnerId,
      spIds: List[SupplyPartnerId],
      offset: Offset,
      limit: Limit)(implicit s: JdbcBackend.Session): List[Version] = {
      def whereVid[A](
        id: SupplyPartnerId,
        q: StaticQuery[Unit, A]) = q + " sp_id = " +? id

      val initial = queryNA[Version]("") +
        "select id, dp_id, sp_id, created, modified, published, max_qps from versions_rules inner join versions on version_id = id where rule_id = " +? id + " AND dp_id = " +? dpId

      spIds.toNel.some { nelIds =>
        // first part of where clause is special as it doesn't start with 'and'
        nelIds.foldLeft(whereVid(nelIds.head, initial + " and")) {
          (acc, id) => whereVid(id, acc + " or ")
        } + " order by published DESC,modified DESC,created DESC limit " +? limit + " offset " +? offset

      }.none(initial).list
    }

    def delete(id: VersionRuleId)(implicit s: JdbcBackend.Session) =
      (u + "delete from versions_rules where rule_id = " +?
        id.ruleId + " and version_id = " +?
        id.versionId).execute
  }

  object Rules {
    def insert(rule: SqlRule)(implicit s: JdbcBackend.Session): Unit =
      (u + "insert into rules (id, name, created, traffic_type) values (" +?
        rule.id + ", " +?
        rule.name + ", " +?
        rule.created + ", " +?
        rule.trafficType + ")").execute

    def get(id: RuleId)(implicit s: JdbcBackend.Session): Option[SqlRule] =
      (queryNA[SqlRule]("") +
        "select id, name, created, traffic_type from rules where id = " +? id).firstOption

    def delete(id: RuleId)(implicit s: JdbcBackend.Session): Unit =
      (u + "delete from rules where id = " +? id).execute

  }
  object RuleConditions {
    def insert(conditions: NonEmptyList[SqlRuleCondition])(implicit s: JdbcBackend.Session) = {
      def appendCondition(q: StaticQuery[Unit, Int], c: SqlRuleCondition) =
        q + "(" +?
          c.id + ", " +?
          c.ruleId + ", " +?
          c.attributeType + ", " +?
          c.defaultAction + ", " +?
          c.undefinedAction + ")"

      val initial =
        appendCondition(
          u + "insert into rule_conditions (id, rule_id, attribute_type, default_action, undefined_action) values ",
          conditions.head)
      conditions.tail.foldLeft(initial) {
        (q, c) => appendCondition(q + ", ", c)
      }.execute
    }

    def listByRuleId(id: RuleId)(implicit s: JdbcBackend.Session) = {
      (queryNA[SqlRuleCondition]("") +
        "select id, rule_id, attribute_type, default_action, undefined_action from rule_conditions where rule_id = " +? id).list
    }

    def deleteByRuleIds(
      ids: NonEmptyList[RuleId])(implicit s: JdbcBackend.Session) = {
      def appendWhere(q: StaticQuery[Unit, Int], id: RuleId) =
        q + "rule_id = " +? id

      val initial =
        appendWhere(
          u + "delete from rule_conditions where ",
          ids.head)
      ids.tail.foldLeft(initial) {
        (q, id) => appendWhere(q + " or ", id)
      }.execute
    }
  }

  object RuleConditionExceptions {
    def insert(
      exceptions: NonEmptyList[SqlRuleConditionException])(implicit s: JdbcBackend.Session) = {
      def appendException(q: StaticQuery[Unit, Int], c: SqlRuleConditionException) =
        q + "(" +?
          c.ruleConditionId + ", " +?
          c.value + ")"
      val initial =
        appendException(
          u + "insert into rule_condition_exceptions (rule_condition_id, feature_id) values ",
          exceptions.head)
      exceptions.tail.foldLeft(initial) {
        (q, e) => appendException(q + ", ", e)
      }.execute
    }

    private def appendWhere[A](q: StaticQuery[Unit, A], id: SqlRuleConditionId) =
      q + "rule_condition_id = " +? id

    def listByRuleConditions(ids: NonEmptyList[SqlRuleConditionId])(implicit s: JdbcBackend.Session) = {
      val initial = appendWhere(
        queryNA[SqlRuleConditionException]("") +
          "select rule_condition_id, feature_id from rule_condition_exceptions where ",
        ids.head)
      ids.tail.foldLeft(initial) {
        (q, id) => appendWhere(q + "or ", id)
      }.list
    }

    def deleteByRuleConditionIds(
      ids: NonEmptyList[SqlRuleConditionId])(implicit s: JdbcBackend.Session) = {
      val initial =
        appendWhere(
          u + "delete from rule_condition_exceptions where ",
          ids.head)
      ids.tail.foldLeft(initial) {
        (q, id) => appendWhere(q + " or ", id)
      }.execute
    }

    def deleteByRuleId(id: RuleId)(implicit s: JdbcBackend.Session) = {
      (u + "delete from rule_condition_exceptions using rule_conditions where rule_id = " +? id).execute
    }
  }

  object UserLists {

    def insert(ul: UserList)(implicit s: JdbcBackend.Session): Unit =
      (u + "insert into user_lists (id, name, dp_id, created, modified) values (" +?
        ul.id + ", " +?
        ul.name + ", " +?
        ul.dpId + ", " +?
        ul.created + ", " +?
        ul.modified + ")").execute

    def get(id: UserListId)(implicit s: JdbcBackend.Session): Option[UserList] =
      (queryNA[UserList]("") +
        "select id, name, dp_id, created, modified from user_lists where id = " +? id + " and enabled = true").firstOption

    def getByName(name: UserListName, dpId: DemandPartnerId)(implicit s: JdbcBackend.Session): Option[UserList] =
      (queryNA[UserList]("") +
        "select id, name, dp_id, created, modified from user_lists where name = " +? name + " and dp_id = " +? dpId + " and enabled = true ").firstOption

    def delete(id: UserListId)(implicit s: JdbcBackend.Session): Unit =
      (u + "delete from user_lists where id = " +? id).execute

    def list(
      dp: Option[DemandPartnerId],
      offset: Offset,
      limit: Limit)(implicit s: JdbcBackend.Session): List[UserList] = {
      type S = StaticQuery[Unit, UserList]
      (for {
        _ <- State.modify[S](_ + "select id, name, dp_id, created, modified from user_lists where enabled = true")
        _ <- dp.traverseS_(id => State.modify[S](_ + " and dp_id = " +? id)) // If not available, traverseS_ comes from scalaz.syntax.foldable._
        _ <- State.modify[S](
          _  + " order by name limit " +? limit + " offset " +? offset)
      } yield ()).exec(queryNA[UserList]("")).list
    }
  }
}

package com.ntoggle.kubitschek
package domainpersistence
package sql

import com.ntoggle.albi._
import com.ntoggle.albi.users.UserListId
import com.ntoggle.albi.TrafficType
import com.ntoggle.goldengate.jodaext.DateTimes
import com.ntoggle.kubitschek.api.Port
import com.ntoggle.kubitschek.domain._
import com.ntoggle.goldengate.Syntax._
import org.joda.time.{DateTimeZone, LocalDateTime, Instant}

import scala.slick.jdbc.{GetResult, PositionedParameters, SetParameter}
import scalaz.syntax.contravariant._
import scalaz.syntax.functor._
import scalaz.{Contravariant, Functor}

/**
 * This is hardcoded against Postgresql because we need to call stored procs and
 * pass arrays as parameters to the stored procs
 */
private[sql] trait SlickMappers {
  this: PostgresSlickDriver =>

  import driver.simple._

  implicit object GetResultFunctor extends Functor[GetResult] {
    def map[A, B](fa: GetResult[A])(f: (A) => B): GetResult[B] =
      GetResult(pr => f(fa.apply(pr)))
  }

  implicit object SetParameterContravariant extends Contravariant[SetParameter] {
    override def contramap[A, B](r: SetParameter[A])(f: (B) => A): SetParameter[B] =
      new SetParameter[B] {
        def apply(v: B, pp: PositionedParameters): Unit = r(f(v), pp)
      }
  }

  def SP[A](implicit evA: SetParameter[A]) = evA
  def spBy[A, B](f: A => B)(implicit evB: SetParameter[B]) = SP[B].contramap(f)
  def GR[A](implicit evA: GetResult[A]) = evA

  implicit val spInstant: SetParameter[Instant] =
    SP[LocalDateTime].contramap(i => DateTimes.dtUtcFromInstant(i.value).toLocalDateTime)
  implicit val grInstant: GetResult[Instant] =
    GetResult(pr => pr.nextLocalDateTime().toDateTime(DateTimeZone.UTC).toInstant)

  implicit val spOptionInstant: SetParameter[Option[Instant]] =
    SP[Option[LocalDateTime]].contramap(
      _.map(i => DateTimes.dtUtcFromInstant(i.value).toLocalDateTime))
  implicit val grOptionInstant: GetResult[Option[Instant]] =
    GetResult(pr =>
      pr.nextLocalDateTimeOption()
        .map(_.toDateTime(DateTimeZone.UTC).toInstant))

  implicit val spMaxQps: SetParameter[MaxQps] = spBy(_.value)
  implicit val grMaxQps: GetResult[MaxQps] = GR[Int].map(MaxQps.apply)

  implicit val spLimit: SetParameter[Limit] = spBy(_.value)
  implicit val spOffset: SetParameter[Offset] = spBy(_.value)

  implicit val spSupplyPartnerId: SetParameter[SupplyPartnerId] = spBy(_.id)
  implicit val grSupplyPartnerId: GetResult[SupplyPartnerId] = GR[String].map(SupplyPartnerId.apply)
  implicit val spDemandPartnerId: SetParameter[DemandPartnerId] = spBy(_.id)
  implicit val grDemandPartnerId: GetResult[DemandPartnerId] = GR[String].map(DemandPartnerId.apply)

  implicit val spSupplyPartnerName: SetParameter[SupplyPartnerName] = spBy(_.value)
  implicit val grSupplyPartnerName: GetResult[SupplyPartnerName] = GR[String].map(SupplyPartnerName.apply)
  implicit val spDemandPartnerName: SetParameter[DemandPartnerName] = spBy(_.value)
  implicit val grDemandPartnerName: GetResult[DemandPartnerName] = GR[String].map(DemandPartnerName.apply)


  implicit val grDemandPartner: GetResult[DemandPartner] =
    GetResult[DemandPartner](dp =>
      DemandPartner(dp.<<, dp.<<))

  implicit val grSupplyPartner: GetResult[SupplyPartner] =
    GetResult[SupplyPartner](dp =>
      SupplyPartner(dp.<<, dp.<<))

  implicit val grRouterConfigurationId: GetResult[RouterConfigurationId] =
    GetResult[RouterConfigurationId](pr =>
      RouterConfigurationId(pr.<<, pr.<<))

  implicit val spVersionId: SetParameter[VersionId] = spBy(_.value)
  implicit val grVersionId: GetResult[VersionId] = GR[String].map(VersionId.apply)

  implicit val spDemandPartnerMaxQps: SetParameter[DemandPartnerMaxQps] =
    spBy(_.value)
  implicit val grDemandPartnerMaxQps: GetResult[DemandPartnerMaxQps] =
    GR[MaxQps].map(DemandPartnerMaxQps.apply)

  implicit val spCreatedInstant: SetParameter[CreatedInstant] = spBy(_.value)
  implicit val grCreatedInstant: GetResult[CreatedInstant] = GR[Instant].map(CreatedInstant.apply)

  implicit val spModifiedInstant: SetParameter[ModifiedInstant] = spBy(_.value)
  implicit val grModifiedInstant: GetResult[ModifiedInstant] = GR[Instant].map(ModifiedInstant.apply)

  implicit val spPublishedInstantOption: SetParameter[Option[PublishedInstant]] =
    spBy(sp => sp.map(_.value))

  implicit val grPublishedInstantOption: GetResult[Option[PublishedInstant]] =
    GR[Option[Instant]].map(_.map(PublishedInstant.apply))

  implicit val grVersion: GetResult[Version] =
    GetResult[Version](pr =>
      Version(
        pr.<<,
        RouterConfigurationId(
          pr.<<,
          pr.<<),
        pr.<<,
        pr.<<,
        pr.<<?,
        pr.<<))

  implicit val grNRouteEndpoint: GetResult[ConfigurationEndpoint] =
    GetResult[ConfigurationEndpoint](pr =>
      ConfigurationEndpoint(pr.<<, Port(pr.<<)))

  implicit val grRuleId: GetResult[RuleId] = GR[String].map(RuleId.apply)
  implicit val spRuleId: SetParameter[RuleId] = spBy(_.value)

  implicit val grDesiredToggledQps: GetResult[DesiredToggledQps] =
    GR[Long].map(DesiredToggledQps.apply)
  implicit val spDesiredToggledQps: SetParameter[DesiredToggledQps] =
    spBy(_.value)

  implicit val grRuleCreatedInstant: GetResult[RuleCreatedInstant] =
    GR[Instant].map(RuleCreatedInstant.apply)
  implicit val spRuleCreatedInstant: SetParameter[RuleCreatedInstant] =
    spBy(_.value)

  implicit val grRuleName: GetResult[RuleName] = GR[String].map(RuleName.apply)
  implicit val spRuleName: SetParameter[RuleName] = spBy(_.value)

  implicit val grRuleStatus: GetResult[RuleStatus] =
    GR[Boolean].map(RuleStatus.fromBoolean)
  implicit val spRuleStatus: SetParameter[RuleStatus] = spBy(_.value)

  implicit val grVersionRuleId: GetResult[VersionRuleId] =
    GetResult[VersionRuleId](pr => VersionRuleId(pr.<<, pr.<<))

  implicit val grVersionRuleSummary: GetResult[VersionRuleSummary] =
    GetResult[VersionRuleSummary](pr =>
      VersionRuleSummary(pr.<<, pr.<<, pr.<<, pr.<<, pr.<<))

  implicit val grVersionRule: GetResult[VersionRule] =
    GetResult[VersionRule](pr =>
      VersionRule(pr.<<, pr.<<))

  implicit val grSqlRuleConditionId: GetResult[SqlRuleConditionId] =
    GR[String].map(SqlRuleConditionId.apply)
  implicit val spSqlRuleConditionId: SetParameter[SqlRuleConditionId] =
    spBy(_.value)

  implicit val grSqlRuleConditionAttributeType: GetResult[SqlRuleConditionAttributeType] =
    GR[String].map(SqlRuleConditionAttributeType.apply)
  implicit val spSqlRuleConditionAttributeType: SetParameter[SqlRuleConditionAttributeType] =
    spBy(_.value)

  implicit val grSqlRuleConditionActionType: GetResult[SqlRuleConditionActionType] =
    GR[Int].map(SqlRuleConditionActionType.fromInt)
  implicit val spSqlRuleConditionActionType: SetParameter[SqlRuleConditionActionType] =
    spBy(SqlRuleConditionActionType.toInt)

  implicit val grSqlRuleConditionException: GetResult[SqlRuleConditionException] =
    GetResult[SqlRuleConditionException](pr =>
      SqlRuleConditionException(pr.<<, pr.<<))

  implicit val grSqlRuleCondition: GetResult[SqlRuleCondition] =
    GetResult[SqlRuleCondition](pr =>
      SqlRuleCondition(pr.<<, pr.<<, pr.<<, pr.<<, pr.<<))

  implicit val grTrafficType: GetResult[TrafficType] =
    GR[String].map(TrafficType.fromStringKey(_).get)
  implicit val spTrafficType: SetParameter[TrafficType] =
    spBy(TrafficType.toStringKey)

  implicit val grSqlRule: GetResult[SqlRule] =
    GetResult[SqlRule](pr => SqlRule.apply(pr.<<, pr.<<, pr.<<, pr.<<))

  implicit val spUserListId: SetParameter[UserListId] = spBy(_.value)
  implicit val grUserListId: GetResult[UserListId] = GR[String].map(UserListId.apply)
  implicit val grUserListName: GetResult[UserListName] = GR[String].map(UserListName.apply)
  implicit val spUserListName: SetParameter[UserListName] = spBy(_.value)

  implicit val grUserList: GetResult[UserList] =
    GetResult[UserList](ul =>
      UserList(ul.<<, ul.<<, ul.<<, ul.<<, ul.<<))

}

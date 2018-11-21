package com.ntoggle.kubitschek.api

import java.util.UUID
import com.ntoggle.albi.TrafficType
import com.ntoggle.albi.users.UserListId
import com.ntoggle.albi.{DemandPartnerId, SupplyPartnerId}
import com.ntoggle.kubitschek.domain.{UserListName, AttributeType, RuleId, VersionId}
import com.ntoggle.humber.catalog.CatalogApi.SuggestRequestString
import com.ntoggle.kubitschek.infra.{ParamExtractorError, ParamExtractor}
import scalaz.syntax.monad._
import scalaz.syntax.std.option._

object ApiParamExtractors {

  object ExpectedTypeNames {
    val TrafficTypes = "'" + TrafficType.Keys.All.mkString("', '") + "'"
    val AttributeTypes = "'" + AttributeType.Keys.All.mkString("', '") + "'"
  }

  implicit val peSupplyPartnerId: ParamExtractor[SupplyPartnerId] =
    ParamExtractor[UUID].map(id => SupplyPartnerId(id.toString))

  implicit val peTrafficType: ParamExtractor[TrafficType] =
    ParamExtractor.from(s => TrafficType.fromStringKey(s)
      .\/>(ParamExtractorError(ExpectedTypeNames.TrafficTypes, s)))

  implicit val peAttributeType: ParamExtractor[AttributeType] =
    ParamExtractor.from(s => AttributeType.fromStringKey(s)
      .\/>(ParamExtractorError(ExpectedTypeNames.AttributeTypes, s)))

  implicit val peSuggestRequestString: ParamExtractor[SuggestRequestString] =
    ParamExtractor[String].map(SuggestRequestString.apply)

  implicit val peDemandPartnerId: ParamExtractor[DemandPartnerId] =
    ParamExtractor[UUID].map(id => DemandPartnerId(id.toString))

  implicit val peVersionId: ParamExtractor[VersionId] =
    ParamExtractor[UUID].map(id => VersionId(id.toString))

  implicit val peRuleId: ParamExtractor[RuleId] =
    ParamExtractor[UUID].map(id => RuleId(id.toString))

  implicit val peUserListId: ParamExtractor[UserListId] =
    ParamExtractor[UUID].map(id => UserListId(id.toString))

  implicit val peUserListName: ParamExtractor[UserListName] =
    ParamExtractor[String].map(id => UserListName(id))


}

package com.ntoggle.kubitschek
package domain

import play.api.libs.json.Json

import scalaz._

case class VersionRuleId(
  versionId: VersionId,
  ruleId: RuleId)
object VersionRuleId {
  implicit val format = Json.format[VersionRuleId]

  val ruleIdLens: VersionRuleId @> RuleId =
    Lens.lensu((r, v) => r.copy(ruleId = v), _.ruleId)

  val versionIdLens: VersionRuleId @> VersionId =
    Lens.lensu((r, v) => r.copy(versionId = v), _.versionId)
}

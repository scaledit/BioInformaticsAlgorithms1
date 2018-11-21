package com.ntoggle.kubitschek.domain

import com.ntoggle.albi.DesiredToggledQps
import com.ntoggle.albi.TrafficType
import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import play.api.libs.json.{Json, Format}

import scalaz.{Lens, @>}

case class VersionRuleSummary(
  id: VersionRuleId,
  trafficType: TrafficType,
  desiredQps: DesiredToggledQps,
  created: RuleCreatedInstant,
  name: RuleName)
object VersionRuleSummary extends EqualzAndShowz[VersionRuleSummary] {
  implicit val formatVersionSummary: Format[VersionRuleSummary] =
    Json.format[VersionRuleSummary]

  val idLens: VersionRuleSummary @> VersionRuleId =
    Lens.lensu((vrs, id) => vrs.copy(id = id), _.id)
  val desiredQpsLens: VersionRuleSummary @> DesiredToggledQps =
    Lens.lensu((vrs, qps) => vrs.copy(desiredQps = qps), _.desiredQps)
}

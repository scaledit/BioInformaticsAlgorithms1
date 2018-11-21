package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.DesiredToggledQps
import com.ntoggle.goldengate.scalazint.EqualzAndShowz
import play.api.libs.json.{Json, Format}

import scalaz.{State, Lens, @>}
import scalaz.std.option._
import scalaz.syntax.traverse._

case class VersionSummary(
  id: VersionId,
  routerConfigurationId: RouterConfigurationId,
  created: CreatedInstant,
  modified: ModifiedInstant,
  published: Option[PublishedInstant],
  maxQps: DemandPartnerMaxQps,
  rules: List[VersionRuleSummary])

object VersionSummary
  extends EqualzAndShowz[VersionSummary] {
  implicit val formatVersionSummary: Format[VersionSummary] =
    Json.format[VersionSummary]

  val rulesLens: VersionSummary @> List[VersionRuleSummary] =
    Lens.lensu((vs, r) => vs.copy(rules = r), _.rules)

  val maxQpsLens: VersionSummary @> DemandPartnerMaxQps =
    Lens.lensu((vs, r) => vs.copy(maxQps = r), _.maxQps)

  def fromVersionAndSummaries(
    version: Version,
    rules: List[VersionRuleSummary]): VersionSummary =
    VersionSummary(
      version.id,
      version.routerConfigurationId,
      version.created,
      version.modified,
      version.published,
      version.maxQps,
      rules)

  def replaceQpsState(
    update: Map[RuleId, DesiredToggledQps]): State[VersionSummary, Unit] =
    rulesLens.mods_ {
      _.map { vrs =>
        update.get(vrs.id.ruleId).traverseS_ {
          qps => VersionRuleSummary.desiredQpsLens := qps
        }.exec(vrs)
      }
    }
}

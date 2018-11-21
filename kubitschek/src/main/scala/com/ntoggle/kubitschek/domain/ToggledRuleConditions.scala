package com.ntoggle.kubitschek
package domain

import com.ntoggle.albi.DesiredToggledQps
import com.ntoggle.albi.TrafficType
import com.ntoggle.goldengate.scalazint.Showz
import monocle.Lens
import monocle.macros.GenLens
import scalaz.std.tuple._
import scalaz.Equal

case class ToggledRuleConditions(
  qps: DesiredToggledQps,
  trafficType: TrafficType,
  conditions: RuleConditions)
object ToggledRuleConditions
  extends Showz[ToggledRuleConditions] {
  implicit val equalToggledRuleConditions: Equal[ToggledRuleConditions] =
    Equal[(DesiredToggledQps, TrafficType, RuleConditions)].contramap(unapply(_).get)

  val _qps: Lens[ToggledRuleConditions, DesiredToggledQps] =
    GenLens[ToggledRuleConditions](_.qps)

  val _conditions: Lens[ToggledRuleConditions, RuleConditions] =
    GenLens[ToggledRuleConditions](_.conditions)
}
